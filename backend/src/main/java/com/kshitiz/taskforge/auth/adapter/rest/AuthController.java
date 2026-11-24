package com.kshitiz.taskforge.auth.adapter.rest;

import com.kshitiz.taskforge.auth.application.AuthService;
import com.kshitiz.taskforge.auth.application.dto.AuthResponse;
import com.kshitiz.taskforge.auth.application.dto.LoginCommand;
import com.kshitiz.taskforge.auth.application.dto.RefreshTokenCommand;
import com.kshitiz.taskforge.auth.application.dto.RegisterCommand;
import com.kshitiz.taskforge.common.adapter.rest.RestResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoints for signing up, logging in and refreshing tokens. They are rate-limited
 * per IP address (see {@code AuthRateLimitFilter}).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
@SecurityRequirements // no token needed: overrides the global bearer requirement in Swagger UI
public class AuthController {

    private final AuthService authService;

    /**
     * Creates a MEMBER account and logs it in.
     *
     * @param command name, email and password
     * @return 201 Created with tokens and the new user's profile
     */
    @PostMapping("/register")
    @Operation(summary = "Sign up (creates a MEMBER)")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterCommand command) {
        AuthResponse response = authService.register(command);
        return RestResponses.created(response, "/api/users/{id}", response.user().id());
    }

    /**
     * Logs in with email and password.
     *
     * @param command credentials
     * @return tokens and profile (401 if the credentials are wrong)
     */
    @PostMapping("/login")
    @Operation(summary = "Log in")
    public AuthResponse login(@Valid @RequestBody LoginCommand command) {
        return authService.login(command);
    }

    /**
     * Exchanges a refresh token for a new token pair.
     *
     * @param command the refresh token
     * @return new tokens and profile (401 if the token is invalid or expired)
     */
    @PostMapping("/refresh")
    @Operation(summary = "Get new tokens with a refresh token")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenCommand command) {
        return authService.refresh(command);
    }
}
