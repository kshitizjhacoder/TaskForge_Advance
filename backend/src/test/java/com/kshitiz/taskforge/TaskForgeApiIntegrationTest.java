package com.kshitiz.taskforge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test of the API against a real MySQL database started in Docker by
 * Testcontainers. It proves that Flyway migrations run, Hibernate's schema validation and
 * the master-data check pass, the bootstrap admin is created, and the main flows work over
 * HTTP with real JWT authentication.
 *
 * <p>Skipped automatically when Docker is not available.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class TaskForgeApiIntegrationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    /** A signed-up user: ID plus tokens. */
    private record Session(String userId, String accessToken, String refreshToken) {
    }

    @Test
    void authFlow() throws Exception {
        Session asha = register("Asha Rao");

        mockMvc.perform(withToken(get("/api/users/me"), asha))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MEMBER"));

        // Refresh gives a new working access token
        JsonNode refreshed = json(mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"%s\"}".formatted(asha.refreshToken())))
                .andExpect(status().isOk()));
        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshed.get("accessToken").asText()))
                .andExpect(status().isOk());

        // Wrong password and missing token
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"nobody@test.dev\", \"password\": \"password123\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/tasks")).andExpect(status().isUnauthorized());
    }

    @Test
    void bootstrapAdminCanManageRoles() throws Exception {
        Session admin = login("admin@test.dev", "admin12345");
        Session ravi = register("Ravi Kumar");

        mockMvc.perform(withToken(put("/api/users/{id}/role", ravi.userId()), admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\": \"MANAGER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MANAGER"));

        // A member cannot change roles
        mockMvc.perform(withToken(put("/api/users/{id}/role", admin.userId()), ravi)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\": \"MEMBER\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void masterDataIsServedFromTheDatabase() throws Exception {
        Session user = register("Reader");

        mockMvc.perform(withToken(get("/api/master"), user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priorities.length()").value(4))
                .andExpect(jsonPath("$['task-statuses'][0].code").value("TO_DO"))
                .andExpect(jsonPath("$.roles[0].code").value("ADMIN"));

        mockMvc.perform(withToken(get("/api/master/unknown"), user)).andExpect(status().isNotFound());
    }

    @Test
    void taskLifecycleWithPermissions() throws Exception {
        Session owner = register("Owner");
        Session other = register("Other member");

        // Create a task: the creator comes from the token
        JsonNode task = json(mockMvc.perform(withToken(post("/api/tasks"), owner)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"title": "Fix login bug", "priority": "HIGH",
                                 "startDate": "2026-12-01", "endDate": "2026-12-05", "tags": ["Backend", "backend", "auth"]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.createdBy").value(owner.userId()))
                .andExpect(jsonPath("$.status").value("TO_DO"))
                .andExpect(jsonPath("$.tags.length()").value(2)));
        String taskId = task.get("id").asText();

        // Another member can neither change its status nor delete it
        mockMvc.perform(withToken(patch("/api/tasks/{id}/status", taskId), other)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"IN_PROGRESS\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(withToken(delete("/api/tasks/{id}", taskId), other)).andExpect(status().isForbidden());

        // Invalid move TO_DO -> DONE is rejected with 422
        mockMvc.perform(withToken(patch("/api/tasks/{id}/status", taskId), owner)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"DONE\"}"))
                .andExpect(status().isUnprocessableEntity());

        // Owner assigns the other member (twice → 409); now the assignee may change the status
        String assignBody = "{\"userId\": \"%s\"}".formatted(other.userId());
        mockMvc.perform(withToken(post("/api/tasks/{id}/assignees", taskId), owner)
                .contentType(MediaType.APPLICATION_JSON).content(assignBody)).andExpect(status().isCreated());
        mockMvc.perform(withToken(post("/api/tasks/{id}/assignees", taskId), owner)
                .contentType(MediaType.APPLICATION_JSON).content(assignBody)).andExpect(status().isConflict());
        mockMvc.perform(withToken(patch("/api/tasks/{id}/status", taskId), other)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.version").value(1));

        // Search by assignee and status
        mockMvc.perform(withToken(get("/api/tasks"), owner).param("assigneeId", other.userId())
                        .param("status", "IN_PROGRESS").param("q", "login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(taskId));

        // Comments: author comes from the token; only the author may edit
        JsonNode comment = json(mockMvc.perform(withToken(post("/api/tasks/{id}/comments", taskId), other)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\": \"Looking into it\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorId").value(other.userId())));
        mockMvc.perform(withToken(put("/api/comments/{id}", comment.get("id").asText()), owner)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\": \"Edited\"}"))
                .andExpect(status().isForbidden());

        // Soft delete by the owner: the task disappears from reads
        mockMvc.perform(withToken(delete("/api/tasks/{id}", taskId), owner)).andExpect(status().isNoContent());
        mockMvc.perform(withToken(get("/api/tasks/{id}", taskId), owner)).andExpect(status().isNotFound());
        mockMvc.perform(withToken(get("/api/users/{id}/assignments", other.userId()), owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void registerValidationListsEachField() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\": \"\", \"email\": \"not-an-email\", \"password\": \"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fullName").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void duplicateEmailIsAConflict() throws Exception {
        String email = "dup-" + UUID.randomUUID() + "@test.dev";
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registerJson("First", email))).andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Second", email)))
                .andExpect(status().isConflict());
    }

    @Test
    void badSortFieldIsABadRequest() throws Exception {
        Session user = register("Sorter");

        mockMvc.perform(withToken(get("/api/tasks"), user).param("sort", "password"))
                .andExpect(status().isBadRequest());
    }

    /** Signs up a new member with a unique email and returns their session. */
    private Session register(String name) throws Exception {
        String email = name.toLowerCase().replace(' ', '.') + "-" + UUID.randomUUID() + "@test.dev";
        return session(mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registerJson(name, email))).andExpect(status().isCreated()));
    }

    /** Logs in and returns the session. */
    private Session login(String email, String password) throws Exception {
        return session(mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, password)))
                .andExpect(status().isOk()));
    }

    /** Reads user ID and tokens from an auth response. */
    private Session session(ResultActions result) throws Exception {
        JsonNode body = json(result);
        return new Session(body.get("user").get("id").asText(), body.get("accessToken").asText(),
                body.get("refreshToken").asText());
    }

    /** Adds the session's access token to a request. */
    private static MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, Session session) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken());
    }

    /** JSON body for sign-up with a fixed test password. */
    private static String registerJson(String name, String email) {
        return """
                {"fullName": "%s", "email": "%s", "password": "password123"}
                """.formatted(name, email);
    }

    /** Parses the response body of a finished request. */
    private JsonNode json(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }
}
