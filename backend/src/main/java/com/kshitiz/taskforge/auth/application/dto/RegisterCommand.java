package com.kshitiz.taskforge.auth.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for public sign-up ({@code POST /api/auth/register}). New accounts always get
 * the MEMBER role; only an admin can grant more.
 *
 * @param fullName display name
 * @param email    login email (must be unique)
 * @param password 8–72 characters (72 is BCrypt's input limit)
 */
public record RegisterCommand(
        @NotBlank @Size(max = 100) String fullName,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(min = 8, max = 72) String password) {

    /**
     * Hides the password if this record is ever logged.
     *
     * @return text form without the password
     */
    @Override
    public String toString() {
        return "RegisterCommand[fullName=" + fullName + ", email=" + email + "]";
    }
}
