package com.kshitiz.taskforge.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * All security settings, bound from the {@code taskforge.security.*} section of
 * {@code application.yml}. Values are checked at startup, so a missing JWT secret stops the
 * application immediately instead of failing on the first login.
 *
 * @param jwt            token signing and lifetimes
 * @param cors           browser origins allowed to call the API
 * @param rateLimit      limits for login/register/refresh requests
 * @param bootstrapAdmin first admin account created at startup
 */
@ConfigurationProperties(prefix = "taskforge.security")
public record SecurityProperties(Jwt jwt, Cors cors, RateLimit rateLimit, BootstrapAdmin bootstrapAdmin) {

    /** Minimum secret length in bytes for HMAC-SHA256 (256 bits). */
    public static final int MIN_SECRET_BYTES = 32;

    /**
     * JWT settings.
     *
     * @param secret          HMAC signing key (at least 32 bytes)
     * @param issuer          value of the token's "iss" claim
     * @param accessTokenTtl  how long an access token is valid, e.g. 15m
     * @param refreshTokenTtl how long a refresh token is valid, e.g. 7d
     */
    public record Jwt(String secret, String issuer, Duration accessTokenTtl, Duration refreshTokenTtl) {

        /**
         * Rejects a missing or too-short secret.
         *
         * @throws IllegalStateException with instructions to set {@code JWT_SECRET}
         */
        public Jwt {
            if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
                throw new IllegalStateException("JWT secret is missing or shorter than " + MIN_SECRET_BYTES
                        + " bytes. Set the JWT_SECRET environment variable.");
            }
        }
    }

    /**
     * CORS settings.
     *
     * @param allowedOrigins frontend URLs, e.g. http://localhost:5173
     */
    public record Cors(List<String> allowedOrigins) {
    }

    /**
     * Rate-limit settings for the authentication endpoints.
     *
     * @param authRequestsPerMinute max attempts per client IP per minute
     */
    public record RateLimit(int authRequestsPerMinute) {
    }

    /**
     * First admin account. Ignored if either email or password is blank.
     *
     * @param email    admin login email
     * @param password admin password
     * @param fullName admin display name
     */
    public record BootstrapAdmin(String email, String password, String fullName) {

        /**
         * Checks whether both credentials were configured.
         *
         * @return {@code true} if an admin should be created when none exists
         */
        public boolean isConfigured() {
            return email != null && !email.isBlank() && password != null && !password.isBlank();
        }
    }
}
