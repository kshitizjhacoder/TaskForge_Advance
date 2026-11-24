package com.kshitiz.taskforge.auth.domain.port;

import com.kshitiz.taskforge.auth.domain.model.IssuedTokens;
import com.kshitiz.taskforge.common.exception.AuthenticationFailedException;
import com.kshitiz.taskforge.common.security.CurrentUser;

import java.util.UUID;

/**
 * Port (interface) for creating and checking tokens. The JWT implementation is
 * {@code JwtTokenProvider}; the rest of the code does not know tokens are JWTs.
 */
public interface TokenProvider {

    /**
     * Creates a new access token and refresh token for a user.
     *
     * @param user the user the tokens belong to
     * @return both tokens and the access token lifetime
     */
    IssuedTokens issueTokens(CurrentUser user);

    /**
     * Checks an access token (signature, expiry, issuer, type) and reads the user from it.
     *
     * @param token token from the Authorization header
     * @return the user the token was issued to
     * @throws AuthenticationFailedException if the token is invalid, expired or a refresh token
     */
    CurrentUser parseAccessToken(String token);

    /**
     * Checks a refresh token and reads the user ID from it.
     *
     * @param token refresh token sent by the client
     * @return ID of the user the token was issued to
     * @throws AuthenticationFailedException if the token is invalid, expired or an access token
     */
    UUID parseRefreshToken(String token);
}
