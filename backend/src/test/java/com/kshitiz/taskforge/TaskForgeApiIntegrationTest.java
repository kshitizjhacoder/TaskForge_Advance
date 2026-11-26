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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test of the API against a real MySQL and a real MinIO (S3) started in Docker by
 * Testcontainers. It proves that Flyway migrations run, Hibernate's schema validation and the
 * master-data check pass, the bootstrap admin is created, and the main flows work over HTTP
 * with real JWT authentication, project membership and file storage.
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

    @Container
    static final MinIOContainer MINIO = new MinIOContainer("minio/minio:RELEASE.2024-10-13T13-34-11Z");

    /**
     * Points the application's S3 settings at the MinIO container.
     *
     * @param registry Spring's registry of test properties
     */
    @DynamicPropertySource
    static void storageProperties(DynamicPropertyRegistry registry) {
        registry.add("taskforge.storage.s3.endpoint", MINIO::getS3URL);
        registry.add("taskforge.storage.s3.access-key", MINIO::getUserName);
        registry.add("taskforge.storage.s3.secret-key", MINIO::getPassword);
        registry.add("taskforge.storage.s3.region", () -> "us-east-1");
        registry.add("taskforge.storage.s3.create-bucket-if-missing", () -> "true");
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    /** A logged-in user: ID plus tokens. */
    private record Session(String userId, String accessToken, String refreshToken) {
    }

    @Test
    void authFlow() throws Exception {
        Session asha = register("Asha Rao");

        mockMvc.perform(withToken(get("/api/users/me"), asha))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MEMBER"));

        Session refreshed = refresh(asha);
        mockMvc.perform(withToken(get("/api/users/me"), refreshed)).andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"nobody@test.dev\", \"password\": \"password123\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/tasks")).andExpect(status().isUnauthorized());
    }

    @Test
    void masterDataIsServedFromTheDatabase() throws Exception {
        Session user = register("Reader");

        mockMvc.perform(withToken(get("/api/master"), user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priorities.length()").value(4))
                .andExpect(jsonPath("$['task-statuses'][0].code").value("TO_DO"))
                .andExpect(jsonPath("$.roles[0].code").value("ADMIN"));
    }

    @Test
    void projectsTasksAndAttachments() throws Exception {
        // An admin promotes a user to MANAGER; after refreshing, the new role is in the token
        Session admin = login("admin@test.dev", "admin12345");
        Session lead = register("Project Lead");
        mockMvc.perform(withToken(put("/api/users/{id}/role", lead.userId()), admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\": \"MANAGER\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(withToken(post("/api/projects"), lead).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"key\": \"X1\", \"name\": \"Too early\"}"))
                .andExpect(status().isForbidden());
        lead = refresh(lead);

        // The manager creates a project and becomes its owner
        String key = "P" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
        String projectId = json(mockMvc.perform(withToken(post("/api/projects"), lead)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"key\": \"%s\", \"name\": \"Website\"}".formatted(key)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(lead.userId()))).get("id").asText();

        Session dev = register("Developer");
        Session outsider = register("Outsider");

        // Non-members cannot even see that the project exists
        mockMvc.perform(withToken(get("/api/projects/{id}", projectId), outsider)).andExpect(status().isNotFound());

        // Owner adds the developer (twice -> 409)
        String addDev = "{\"userId\": \"%s\"}".formatted(dev.userId());
        mockMvc.perform(withToken(post("/api/projects/{id}/members", projectId), lead)
                .contentType(MediaType.APPLICATION_JSON).content(addDev)).andExpect(status().isCreated());
        mockMvc.perform(withToken(post("/api/projects/{id}/members", projectId), lead)
                .contentType(MediaType.APPLICATION_JSON).content(addDev)).andExpect(status().isConflict());

        // The developer creates a task; the outsider cannot
        String taskBody = """
                {"projectId": "%s", "title": "Fix login bug", "priority": "HIGH", "tags": ["Backend", "backend"]}
                """.formatted(projectId);
        String taskId = json(mockMvc.perform(withToken(post("/api/tasks"), dev)
                        .contentType(MediaType.APPLICATION_JSON).content(taskBody))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.createdBy").value(dev.userId()))
                .andExpect(jsonPath("$.tags.length()").value(1))).get("id").asText();
        mockMvc.perform(withToken(post("/api/tasks"), outsider).contentType(MediaType.APPLICATION_JSON)
                .content(taskBody)).andExpect(status().isNotFound());

        // Only project members can be assigned
        mockMvc.perform(withToken(post("/api/tasks/{id}/assignees", taskId), lead)
                        .contentType(MediaType.APPLICATION_JSON).content(addDev))
                .andExpect(status().isCreated());
        mockMvc.perform(withToken(post("/api/tasks/{id}/assignees", taskId), lead)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": \"%s\"}".formatted(outsider.userId())))
                .andExpect(status().isUnprocessableEntity());

        // Workflow rules still apply
        mockMvc.perform(withToken(patch("/api/tasks/{id}/status", taskId), dev)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"DONE\"}"))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(withToken(patch("/api/tasks/{id}/status", taskId), dev)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"IN_PROGRESS\"}"))
                .andExpect(status().isOk());

        // Search only covers the user's projects
        mockMvc.perform(withToken(get("/api/tasks"), dev).param("assigneeId", dev.userId()))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(withToken(get("/api/tasks"), outsider)).andExpect(jsonPath("$.totalElements").value(0));

        // Attachments: upload, list, download, reject a bad type, delete
        byte[] bytes = "steps to reproduce".getBytes(StandardCharsets.UTF_8);
        String attachmentId = json(mockMvc.perform(withToken(multipart("/api/tasks/{id}/attachments", taskId)
                        .file(new MockMultipartFile("file", "../notes.txt", "text/plain", bytes)), dev))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("notes.txt"))
                .andExpect(jsonPath("$.sizeBytes").value(bytes.length))).get("id").asText();
        mockMvc.perform(withToken(get("/api/tasks/{id}/attachments", taskId), lead))
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(withToken(get("/api/attachments/{id}/content", attachmentId), lead))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(content().bytes(bytes));
        mockMvc.perform(withToken(get("/api/attachments/{id}/content", attachmentId), outsider))
                .andExpect(status().isNotFound());
        mockMvc.perform(withToken(multipart("/api/tasks/{id}/attachments", taskId)
                        .file(new MockMultipartFile("file", "run.exe", "application/x-msdownload", bytes)), dev))
                .andExpect(status().isBadRequest());
        mockMvc.perform(withToken(delete("/api/attachments/{id}", attachmentId), dev))
                .andExpect(status().isNoContent());

        // Removing the developer also removes their assignments and their access
        mockMvc.perform(withToken(delete("/api/projects/{id}/members/{userId}", projectId, lead.userId()), lead))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(withToken(delete("/api/projects/{id}/members/{userId}", projectId, dev.userId()), lead))
                .andExpect(status().isNoContent());
        mockMvc.perform(withToken(get("/api/tasks/{id}/assignees", taskId), lead))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(withToken(get("/api/tasks/{id}", taskId), dev)).andExpect(status().isNotFound());

        // Deleting the project hides its tasks
        mockMvc.perform(withToken(delete("/api/projects/{id}", projectId), lead)).andExpect(status().isNoContent());
        mockMvc.perform(withToken(get("/api/tasks/{id}", taskId), lead)).andExpect(status().isNotFound());
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
        String email = name.toLowerCase(Locale.ROOT).replace(' ', '.') + "-" + UUID.randomUUID() + "@test.dev";
        return session(mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registerJson(name, email))).andExpect(status().isCreated()));
    }

    /** Logs in and returns the session. */
    private Session login(String email, String password) throws Exception {
        return session(mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, password)))
                .andExpect(status().isOk()));
    }

    /** Exchanges the session's refresh token for new tokens (picks up role changes). */
    private Session refresh(Session session) throws Exception {
        return session(mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"%s\"}".formatted(session.refreshToken())))
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
