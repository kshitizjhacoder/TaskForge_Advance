package com.kshitiz.taskforge.auth.adapter.security;

import com.kshitiz.taskforge.auth.config.SecurityProperties;
import com.kshitiz.taskforge.auth.domain.port.TokenProvider;
import com.kshitiz.taskforge.common.adapter.rest.ProblemDetailWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

/**
 * Security rules for the whole API.
 *
 * <ul>
 *   <li><b>Stateless</b>: no server session or cookies; every request carries a JWT, so CSRF
 *       protection (a cookie-based attack) is not needed.</li>
 *   <li><b>Public</b>: sign-up, login, refresh, health check and API docs.</li>
 *   <li><b>Everything else</b> needs a valid access token. Finer rules (e.g. ADMIN only) are
 *       on controller methods with {@code @PreAuthorize}, and ownership rules are checked in
 *       services.</li>
 * </ul>
 * Filter order: rate limit → JWT authentication → Spring's own filters.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** URLs anyone may call without logging in. */
    private static final String[] PUBLIC_URLS = {
            "/api/auth/**",
            "/actuator/health/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/error"
    };

    /**
     * Builds the security filter chain.
     *
     * @param http                Spring Security's configuration builder
     * @param tokenProvider       checks access tokens
     * @param problemDetailWriter writes JSON errors from filters
     * @param properties          security settings
     * @return the configured chain
     * @throws Exception if the configuration is invalid
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, TokenProvider tokenProvider,
                                                   ProblemDetailWriter problemDetailWriter,
                                                   SecurityProperties properties) throws Exception {
        SecurityProblemHandler problemHandler = new SecurityProblemHandler(problemDetailWriter);
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(tokenProvider);
        AuthRateLimitFilter rateLimitFilter = new AuthRateLimitFilter(problemDetailWriter,
                properties.rateLimit().authRequestsPerMinute());

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource(properties)))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(problemHandler)
                        .accessDeniedHandler(problemHandler))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitFilter, JwtAuthenticationFilter.class)
                .build();
    }

    /**
     * Password hasher. BCrypt adds a random salt and is deliberately slow, which makes stolen
     * hashes hard to crack.
     *
     * @return the BCrypt password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Allows the frontend (on another origin, e.g. http://localhost:5173) to call the API
     * from the browser. Only the configured origins are allowed.
     *
     * @param properties security settings with the allowed origins
     * @return CORS rules for all {@code /api/**} URLs
     */
    private CorsConfigurationSource corsConfigurationSource(SecurityProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(properties.cors().allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
        config.setExposedHeaders(List.of(HttpHeaders.LOCATION));
        config.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
