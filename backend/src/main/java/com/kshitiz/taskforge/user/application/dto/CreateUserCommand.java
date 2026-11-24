package com.kshitiz.taskforge.user.application.dto;

import com.kshitiz.taskforge.user.domain.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating a user ({@code POST /api/users}).
 *
 * <p>The validation annotations are checked automatically when a controller receives the
 * body with {@code @Valid}; failures become a 400 response listing each bad field.
 *
 * @param fullName display name
 * @param email    login email (must be unique)
 * @param password plain-text password, hashed before saving; 8–72 characters
 *                 (72 is BCrypt's input limit)
 * @param role     application role
 */
public record CreateUserCommand(
        @NotBlank @Size(max = 100) String fullName,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull Role role) {

    /**
     * Hides the password if this record is ever logged or printed.
     *
     * @return text form without the password
     */
    @Override
    public String toString() {
        return "CreateUserCommand[fullName=" + fullName + ", email=" + email + ", role=" + role + "]";
    }
}
