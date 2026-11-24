package com.kshitiz.taskforge.auth.adapter.security;

import com.kshitiz.taskforge.common.adapter.rest.ProblemDetailWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

/**
 * Answers requests that Spring Security rejects before they reach a controller, using the
 * same JSON error format as the rest of the API:
 * <ul>
 *   <li>no valid token on a protected endpoint → 401;</li>
 *   <li>valid token but the URL rule denies it → 403.</li>
 * </ul>
 */
@RequiredArgsConstructor
public class SecurityProblemHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ProblemDetailWriter problemDetailWriter;

    /**
     * Sends 401 when the user is not authenticated.
     *
     * @param request       rejected request
     * @param response      response to write
     * @param authException reason given by Spring Security
     * @throws IOException if the response cannot be written
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        String detail = request.getAttribute(JwtAuthenticationFilter.INVALID_TOKEN_ATTRIBUTE) != null
                ? "Invalid or expired token. Log in again or refresh your token."
                : "Authentication required. Send a Bearer access token.";
        problemDetailWriter.write(request, response, HttpStatus.UNAUTHORIZED, detail);
    }

    /**
     * Sends 403 when the user is authenticated but not allowed.
     *
     * @param request               rejected request
     * @param response              response to write
     * @param accessDeniedException reason given by Spring Security
     * @throws IOException if the response cannot be written
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        problemDetailWriter.write(request, response, HttpStatus.FORBIDDEN,
                "You do not have permission to perform this action.");
    }
}
