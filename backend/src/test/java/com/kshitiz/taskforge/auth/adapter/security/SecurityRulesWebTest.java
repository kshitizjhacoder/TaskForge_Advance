package com.kshitiz.taskforge.auth.adapter.security;

import com.kshitiz.taskforge.auth.adapter.rest.AuthController;
import com.kshitiz.taskforge.auth.application.AuthService;
import com.kshitiz.taskforge.auth.application.dto.AuthResponse;
import com.kshitiz.taskforge.auth.config.SecurityProperties;
import com.kshitiz.taskforge.common.adapter.rest.ProblemDetailWriter;
import com.kshitiz.taskforge.common.security.CurrentUser;
import com.kshitiz.taskforge.project.adapter.rest.ProjectController;
import com.kshitiz.taskforge.project.application.ProjectService;
import com.kshitiz.taskforge.task.adapter.rest.TaskController;
import com.kshitiz.taskforge.task.application.TaskService;
import com.kshitiz.taskforge.task.application.dto.TaskResponse;
import com.kshitiz.taskforge.task.domain.model.Priority;
import com.kshitiz.taskforge.task.domain.model.TaskStatus;
import com.kshitiz.taskforge.user.adapter.rest.UserController;
import com.kshitiz.taskforge.user.application.UserService;
import com.kshitiz.taskforge.user.application.dto.UserResponse;
import com.kshitiz.taskforge.user.domain.model.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests the security rules through the real filter chain (JWT filter, entry point, method
 * security) with mocked services. Starts only the web layer, so it needs no database or
 * Docker and runs in a few seconds.
 */
@WebMvcTest(controllers = {AuthController.class, UserController.class, TaskController.class,
        ProjectController.class})
@Import({SecurityConfig.class, JwtTokenProvider.class, ProblemDetailWriter.class})
@EnableConfigurationProperties(SecurityProperties.class)
@ActiveProfiles("test")
class SecurityRulesWebTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtTokenProvider tokenProvider;

    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private TaskService taskService;
    @MockitoBean
    private ProjectService projectService;

    private final CurrentUser member = new CurrentUser(UUID.randomUUID(), "member@test.dev", Role.MEMBER);
    private final CurrentUser admin = new CurrentUser(UUID.randomUUID(), "admin@test.dev", Role.ADMIN);

    /** Builds an Authorization header value with a fresh access token for {@code user}. */
    private String bearer(CurrentUser user) {
        return "Bearer " + tokenProvider.issueTokens(user).accessToken();
    }

    /** Builds a user response for mocked services. */
    private static UserResponse userResponse(CurrentUser user) {
        return new UserResponse(user.id(), "Name", user.email(), user.role(), null, Instant.now(), Instant.now());
    }

    @Test
    void protectedEndpointWithoutTokenIs401ProblemJson() throws Exception {
        mockMvc.perform(get("/api/tasks/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Authentication required. Send a Bearer access token."));
        verifyNoInteractions(taskService);
    }

    @Test
    void invalidTokenIs401WithClearMessage() throws Exception {
        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid or expired token. Log in again or refresh your token."));
    }

    @Test
    void refreshTokenCannotBeUsedForApiCalls() throws Exception {
        String refreshToken = tokenProvider.issueTokens(member).refreshToken();

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginIsPublic() throws Exception {
        when(authService.login(any())).thenReturn(new AuthResponse("a", "r", "Bearer", 900, userResponse(member)));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"member@test.dev\", \"password\": \"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("a"));
    }

    @Test
    void meReturnsTheTokenOwner() throws Exception {
        when(userService.getUser(member.id())).thenReturn(userResponse(member));

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer(member)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("member@test.dev"));
    }

    @Test
    void memberCannotCreateUsersButAdminCan() throws Exception {
        String body = """
                {"fullName": "New", "email": "new@test.dev", "password": "password123", "role": "MANAGER"}
                """;
        mockMvc.perform(post("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(member))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        verifyNoInteractions(userService);

        CurrentUser created = new CurrentUser(UUID.randomUUID(), "new@test.dev", Role.MANAGER);
        when(userService.createUser(any())).thenReturn(userResponse(created));
        mockMvc.perform(post("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void createTaskUsesTheLoggedInUserAsCreator() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(taskService.createTask(any(), eq(member))).thenReturn(new TaskResponse(UUID.randomUUID(), projectId, null,
                "T", null, member.id(), null, null, Priority.MEDIUM, TaskStatus.TO_DO, List.of(), Instant.now(),
                Instant.now(), 0L));

        mockMvc.perform(post("/api/tasks").header(HttpHeaders.AUTHORIZATION, bearer(member))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"T\", \"projectId\": \"%s\"}".formatted(projectId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdBy").value(member.id().toString()));
        verify(taskService).createTask(any(), eq(member));
    }

    @Test
    void createTaskWithoutProjectIsABadRequest() throws Exception {
        mockMvc.perform(post("/api/tasks").header(HttpHeaders.AUTHORIZATION, bearer(member))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\": \"T\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.projectId").exists());
        verifyNoInteractions(taskService);
    }

    @Test
    void membersCannotCreateProjects() throws Exception {
        mockMvc.perform(post("/api/projects").header(HttpHeaders.AUTHORIZATION, bearer(member))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"key\": \"TF\", \"name\": \"TaskForge\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(projectService);
    }
}
