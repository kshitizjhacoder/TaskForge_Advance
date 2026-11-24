package com.kshitiz.taskforge.auth.adapter.security;

import com.kshitiz.taskforge.auth.config.SecurityProperties;
import com.kshitiz.taskforge.auth.domain.model.IssuedTokens;
import com.kshitiz.taskforge.auth.domain.port.TokenProvider;
import com.kshitiz.taskforge.common.exception.AuthenticationFailedException;
import com.kshitiz.taskforge.common.security.CurrentUser;
import com.kshitiz.taskforge.user.domain.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Creates and verifies JSON Web Tokens with the jjwt library, signed with HMAC-SHA256.
 *
 * <p>Token contents ("claims"):
 * <ul>
 *   <li>{@code sub} – user ID; {@code iss} – "taskforge"; {@code jti} – random token ID;</li>
 *   <li>{@code iat}/{@code exp} – issued-at and expiry times;</li>
 *   <li>{@code type} – ACCESS or REFRESH, so one can never be used as the other;</li>
 *   <li>{@code email}, {@code role} – only in access tokens, so requests need no DB lookup.</li>
 * </ul>
 * Refresh tokens carry only the user ID: the refresh endpoint reloads the user from the
 * database, so role changes and deletions take effect at the next refresh.
 */
@Component
public class JwtTokenProvider implements TokenProvider {

    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";
    private static final String INVALID_TOKEN = "Invalid or expired token";

    /** Kind of token, stored in the "type" claim. */
    private enum TokenType { ACCESS, REFRESH }

    private final SecretKey signingKey;
    private final String issuer;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;
    private final Clock clock;

    /**
     * Creates the provider used by the application (real system clock). {@code @Autowired}
     * tells Spring to use this constructor, since the class has two.
     *
     * @param properties security settings with the secret and lifetimes
     */
    @Autowired
    public JwtTokenProvider(SecurityProperties properties) {
        this(properties.jwt(), Clock.systemUTC());
    }

    /**
     * Creates a provider with a chosen clock, so tests can check expiry without waiting.
     *
     * @param jwt   JWT settings
     * @param clock source of the current time
     */
    JwtTokenProvider(SecurityProperties.Jwt jwt, Clock clock) {
        this.signingKey = Keys.hmacShaKeyFor(jwt.secret().getBytes(StandardCharsets.UTF_8));
        this.issuer = jwt.issuer();
        this.accessTokenTtl = jwt.accessTokenTtl();
        this.refreshTokenTtl = jwt.refreshTokenTtl();
        this.clock = clock;
    }

    /** {@inheritDoc} */
    @Override
    public IssuedTokens issueTokens(CurrentUser user) {
        String accessToken = buildToken(user.id(), TokenType.ACCESS, accessTokenTtl,
                Map.of(CLAIM_EMAIL, user.email(), CLAIM_ROLE, user.role().name()));
        String refreshToken = buildToken(user.id(), TokenType.REFRESH, refreshTokenTtl, Map.of());
        return new IssuedTokens(accessToken, refreshToken, accessTokenTtl);
    }

    /**
     * Builds and signs one token with the standard claims plus any extra claims.
     *
     * @param userId      user the token belongs to (becomes "sub")
     * @param type        ACCESS or REFRESH
     * @param ttl         how long the token is valid
     * @param extraClaims additional claims, e.g. email and role
     * @return the signed, compact JWT string
     */
    private String buildToken(UUID userId, TokenType type, Duration ttl, Map<String, String> extraClaims) {
        Instant now = clock.instant();
        return Jwts.builder()
                .issuer(issuer)
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .claim(CLAIM_TYPE, type.name())
                .claims(extraClaims)
                .signWith(signingKey)
                .compact();
    }

    /** {@inheritDoc} */
    @Override
    public CurrentUser parseAccessToken(String token) {
        Claims claims = parse(token, TokenType.ACCESS);
        try {
            return new CurrentUser(UUID.fromString(claims.getSubject()), claims.get(CLAIM_EMAIL, String.class),
                    Role.valueOf(claims.get(CLAIM_ROLE, String.class)));
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new AuthenticationFailedException(INVALID_TOKEN);
        }
    }

    /** {@inheritDoc} */
    @Override
    public UUID parseRefreshToken(String token) {
        Claims claims = parse(token, TokenType.REFRESH);
        try {
            return UUID.fromString(claims.getSubject());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new AuthenticationFailedException(INVALID_TOKEN);
        }
    }

    /**
     * Verifies signature, issuer and expiry, then checks the token is of the expected type.
     *
     * @param token        raw token
     * @param expectedType ACCESS or REFRESH
     * @return the token's claims
     * @throws AuthenticationFailedException if any check fails
     */
    private Claims parse(String token, TokenType expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(issuer)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!expectedType.name().equals(claims.get(CLAIM_TYPE, String.class))) {
                throw new AuthenticationFailedException(INVALID_TOKEN);
            }
            return claims;
        } catch (JwtException | IllegalArgumentException ex) {
            throw new AuthenticationFailedException(INVALID_TOKEN);
        }
    }
}
