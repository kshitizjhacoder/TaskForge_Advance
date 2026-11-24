package com.kshitiz.taskforge.auth.domain.model;

import java.time.Duration;

/**
 * The pair of tokens given to a user after login, sign-up or refresh.
 *
 * @param accessToken          short-lived token sent with every API request
 * @param refreshToken         long-lived token used only to get a new pair
 * @param accessTokenExpiresIn how long the access token stays valid
 */
public record IssuedTokens(String accessToken, String refreshToken, Duration accessTokenExpiresIn) {
}
