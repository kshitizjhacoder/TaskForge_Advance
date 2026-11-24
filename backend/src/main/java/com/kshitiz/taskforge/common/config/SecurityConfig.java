package com.kshitiz.taskforge.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Temporary security setup for Phase 1: every endpoint is open.
 *
 * <p>Phase 2 replaces this with JWT authentication and role checks in the {@code auth}
 * feature. Until then this class exists so the API is usable (Spring Security's default
 * would demand a generated password and block every POST).
 */
@Configuration
public class SecurityConfig {

    /**
     * Defines the HTTP security rules: stateless, no CSRF (the API uses no cookies), and
     * all requests allowed.
     *
     * @param http Spring Security's builder
     * @return the configured filter chain
     * @throws Exception if the configuration is invalid
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }

    /**
     * Password hasher used when users are created (and, from Phase 2, when they log in).
     * BCrypt adds a random salt and is deliberately slow, which makes stolen hashes hard to
     * crack.
     *
     * @return the BCrypt password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
