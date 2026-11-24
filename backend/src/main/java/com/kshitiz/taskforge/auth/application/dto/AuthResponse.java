package com.kshitiz.taskforge.auth.application.dto;

import com.kshitiz.taskforge.auth.domain.model.IssuedTokens;
import com.kshitiz.taskforge.user.application.dto.UserResponse;

/**
 * Response of sign-up, login and refresh.
 *
 * <p>The client sends {@code accessToken} as {@code Authorization: Bearer <token>} on every
 * request. When it expires (after {@code expiresIn} seconds) the client calls
 * {@code /api/auth/refresh} with {@code refreshToken} to get a new pair.
 *
 * @param accessToken  short-lived token for API calls
 * @param refreshToken long-lived token for getting new tokens
 * @param tokenType    always "Bearer"
 * @param expiresIn    access token lifetime in seconds
 * @param user         the logged-in user's profile
 */
public record AuthResponse(String accessToken, String refreshToken, String tokenType, long expiresIn,
                           UserResponse user) {

    /**
     * Combines issued tokens and the user profile into a response.
     *
     * @param tokens tokens from the token provider
     * @param user   the user's profile
     * @return the response
     */
    public static AuthResponse of(IssuedTokens tokens, UserResponse user) {
        return new AuthResponse(tokens.accessToken(), tokens.refreshToken(), "Bearer",
                tokens.accessTokenExpiresIn().toSeconds(), user);
    }
}
