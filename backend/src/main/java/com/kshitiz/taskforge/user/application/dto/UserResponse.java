package com.kshitiz.taskforge.user.application.dto;

import com.kshitiz.taskforge.user.domain.model.Role;
import com.kshitiz.taskforge.user.domain.model.User;

import java.time.Instant;
import java.util.UUID;

/**
 * JSON shape of a user returned by the API. Deliberately leaves out the password hash.
 *
 * @param id        user ID
 * @param fullName  display name
 * @param email     login email
 * @param role      application role
 * @param managerId manager's ID, or {@code null}
 * @param createdAt when the user was created
 * @param updatedAt when the user was last changed
 */
public record UserResponse(UUID id, String fullName, String email, Role role, UUID managerId,
                           Instant createdAt, Instant updatedAt) {

    /**
     * Converts a domain user into its API representation.
     *
     * @param user domain object
     * @return response DTO
     */
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole(),
                user.getManagerId(), user.getCreatedAt(), user.getUpdatedAt());
    }
}
