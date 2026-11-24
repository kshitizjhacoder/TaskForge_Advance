package com.kshitiz.taskforge.auth.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/auth/login}.
 *
 * @param email    login email
 * @param password password
 */
public record LoginCommand(
        @NotBlank @Size(max = 150) String email,
        @NotBlank @Size(max = 72) String password) {

    /**
     * Hides the password if this record is ever logged.
     *
     * @return text form without the password
     */
    @Override
    public String toString() {
        return "LoginCommand[email=" + email + "]";
    }
}
