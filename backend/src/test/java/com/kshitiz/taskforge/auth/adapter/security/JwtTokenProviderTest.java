package com.kshitiz.taskforge.auth.adapter.security;

import com.kshitiz.taskforge.auth.config.SecurityProperties;
import com.kshitiz.taskforge.auth.domain.model.IssuedTokens;
import com.kshitiz.taskforge.common.exception.AuthenticationFailedException;
import com.kshitiz.taskforge.common.security.CurrentUser;
import com.kshitiz.taskforge.user.domain.model.Role;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link JwtTokenProvider}: round trip, expiry, token-type confusion,
 * tampering and wrong key.
 */
class JwtTokenProviderTest {

    private static final String SECRET = "unit-test-secret-unit-test-secret-0123456789";
    private static final Instant NOW = Instant.parse("2026-12-01T10:00:00Z");
    private static final CurrentUser USER = new CurrentUser(UUID.randomUUID(), "asha@test.dev", Role.MANAGER);

    /** Creates a provider whose clock is frozen at {@code instant}. */
    private static JwtTokenProvider providerAt(Instant instant, String secret) {
        var jwt = new SecurityProperties.Jwt(secret, "taskforge", Duration.ofMinutes(15), Duration.ofDays(7));
        return new JwtTokenProvider(jwt, Clock.fixed(instant, ZoneOffset.UTC));
    }

    @Test
    void accessTokenRoundTrip() {
        JwtTokenProvider provider = providerAt(NOW, SECRET);

        IssuedTokens tokens = provider.issueTokens(USER);

        assertThat(provider.parseAccessToken(tokens.accessToken())).isEqualTo(USER);
        assertThat(provider.parseRefreshToken(tokens.refreshToken())).isEqualTo(USER.id());
        assertThat(tokens.accessTokenExpiresIn()).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void expiredAccessTokenIsRejected() {
        String token = providerAt(NOW, SECRET).issueTokens(USER).accessToken();

        JwtTokenProvider sixteenMinutesLater = providerAt(NOW.plus(Duration.ofMinutes(16)), SECRET);

        assertThatThrownBy(() -> sixteenMinutesLater.parseAccessToken(token))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void refreshTokenCannotBeUsedAsAccessTokenAndViceVersa() {
        JwtTokenProvider provider = providerAt(NOW, SECRET);
        IssuedTokens tokens = provider.issueTokens(USER);

        assertThatThrownBy(() -> provider.parseAccessToken(tokens.refreshToken()))
                .isInstanceOf(AuthenticationFailedException.class);
        assertThatThrownBy(() -> provider.parseRefreshToken(tokens.accessToken()))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtTokenProvider provider = providerAt(NOW, SECRET);
        String token = provider.issueTokens(USER).accessToken();
        String tampered = token.substring(0, token.length() - 4) + "AAAA";

        assertThatThrownBy(() -> provider.parseAccessToken(tampered))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String token = providerAt(NOW, "another-secret-another-secret-0123456789").issueTokens(USER).accessToken();

        assertThatThrownBy(() -> providerAt(NOW, SECRET).parseAccessToken(token))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void garbageIsRejected() {
        assertThatThrownBy(() -> providerAt(NOW, SECRET).parseAccessToken("not-a-jwt"))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void shortSecretFailsAtStartup() {
        assertThatThrownBy(() -> new SecurityProperties.Jwt("too-short", "taskforge", Duration.ofMinutes(1),
                Duration.ofDays(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }
}
