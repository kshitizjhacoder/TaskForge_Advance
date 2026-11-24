package com.kshitiz.taskforge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test of the API against a real MySQL database started in Docker by
 * Testcontainers. It proves that Flyway migrations run, Hibernate's schema validation
 * passes, the master-data check passes, and the main flows work through HTTP.
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

    @Test
    void masterDataIsServedFromTheDatabase() throws Exception {
        mockMvc.perform(get("/api/master"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priorities.length()").value(4))
                .andExpect(jsonPath("$['task-statuses'][0].code").value("TO_DO"))
                .andExpect(jsonPath("$.roles[0].code").value("ADMIN"));

        mockMvc.perform(get("/api/master/unknown")).andExpect(status().isNotFound());
    }

    @Test
    void taskLifecycle() throws Exception {
        String userId = createUser("Asha Rao", "asha-" + UUID.randomUUID() + "@test.dev");

        // Create a task
        JsonNode task = json(mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content("""
                        {"title": "Fix login bug", "createdBy": "%s", "priority": "HIGH",
                         "startDate": "2026-12-01", "endDate": "2026-12-05", "tags": ["Backend", "backend", "auth"]}
                        """.formatted(userId)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("TO_DO"))
                .andExpect(jsonPath("$.tags.length()").value(2)));
        String taskId = task.get("id").asText();

        // Invalid move TO_DO -> DONE is rejected with 422
        mockMvc.perform(patch("/api/tasks/{id}/status", taskId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"DONE\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").exists());

        // Valid move TO_DO -> IN_PROGRESS
        mockMvc.perform(patch("/api/tasks/{id}/status", taskId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.version").value(1));

        // Assign the user, twice (second time is a conflict)
        String assignBody = "{\"userId\": \"%s\"}".formatted(userId);
        mockMvc.perform(post("/api/tasks/{id}/assignees", taskId).contentType(MediaType.APPLICATION_JSON)
                .content(assignBody)).andExpect(status().isCreated());
        mockMvc.perform(post("/api/tasks/{id}/assignees", taskId).contentType(MediaType.APPLICATION_JSON)
                .content(assignBody)).andExpect(status().isConflict());

        // Search by assignee and status
        mockMvc.perform(get("/api/tasks").param("assigneeId", userId).param("status", "IN_PROGRESS")
                        .param("q", "login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(taskId));

        // Comment on the task
        mockMvc.perform(post("/api/tasks/{id}/comments", taskId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorId\": \"%s\", \"content\": \"Looking into it\"}".formatted(userId)))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/tasks/{id}/comments", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Looking into it"));

        // Soft delete: the task disappears from reads
        mockMvc.perform(delete("/api/tasks/{id}", taskId)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/tasks/{id}", taskId)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/users/{id}/assignments", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void validationErrorsListEachField() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\": \"\", \"email\": \"not-an-email\", \"password\": \"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fullName").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.role").exists());
    }

    @Test
    void duplicateEmailIsAConflict() throws Exception {
        String email = "dup-" + UUID.randomUUID() + "@test.dev";
        createUser("First", email);

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(userJson("Second", email)))
                .andExpect(status().isConflict());
    }

    @Test
    void badSortFieldIsABadRequest() throws Exception {
        mockMvc.perform(get("/api/tasks").param("sort", "password"))
                .andExpect(status().isBadRequest());
    }

    /** Creates a user through the API and returns its ID. */
    private String createUser(String name, String email) throws Exception {
        return json(mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content(userJson(name, email)))
                .andExpect(status().isCreated())).get("id").asText();
    }

    /** JSON body for creating a member with a fixed test password. */
    private static String userJson(String name, String email) {
        return """
                {"fullName": "%s", "email": "%s", "password": "password123", "role": "MEMBER"}
                """.formatted(name, email);
    }

    /** Parses the response body of a finished request. */
    private JsonNode json(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }
}
