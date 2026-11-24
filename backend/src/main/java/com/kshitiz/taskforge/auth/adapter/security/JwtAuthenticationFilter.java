package com.kshitiz.taskforge.auth.adapter.security;

import com.kshitiz.taskforge.auth.domain.port.TokenProvider;
import com.kshitiz.taskforge.common.exception.AuthenticationFailedException;
import com.kshitiz.taskforge.common.security.CurrentUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Runs once per request: reads the {@code Authorization: Bearer <token>} header, checks the
 * access token and, if valid, tells Spring Security who the user is and which role they have.
 *
 * <p>If the header is missing or the token is bad, the request simply continues
 * unauthenticated; Spring Security then answers 401 for protected endpoints (see
 * {@link SecurityProblemHandler}). Public endpoints such as login keep working.
 *
 * <p>Not a Spring {@code @Component} on purpose: it is added to the security chain in
 * {@link SecurityConfig}, and registering it as a bean would make it run twice.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Request attribute telling the 401 handler that a token was sent but rejected. */
    static final String INVALID_TOKEN_ATTRIBUTE = "taskforge.invalidToken";

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenProvider tokenProvider;

    /**
     * Authenticates the request from its bearer token, then passes it on.
     *
     * @param request     incoming request
     * @param response    outgoing response
     * @param filterChain remaining filters
     * @throws ServletException if a later filter fails
     * @throws IOException      if a later filter fails
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            try {
                CurrentUser user = tokenProvider.parseAccessToken(header.substring(BEARER_PREFIX.length()).trim());
                var authentication = new UsernamePasswordAuthenticationToken(user, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name())));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (AuthenticationFailedException ex) {
                request.setAttribute(INVALID_TOKEN_ATTRIBUTE, Boolean.TRUE);
            }
        }
        filterChain.doFilter(request, response);
    }
}
