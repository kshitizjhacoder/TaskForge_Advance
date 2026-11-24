package com.kshitiz.taskforge.auth.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/auth/refresh}.
 *
 * @param refreshToken the refresh token received at login
 */
public record RefreshTokenCommand(@NotBlank String refreshToken) {

    /**
     * Hides the token if this record is ever logged.
     *
     * @return text form without the token
     */
    @Override
    public String toString() {
        return "RefreshTokenCommand[refreshToken=***]";
    }
}
