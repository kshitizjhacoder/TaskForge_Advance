package com.kshitiz.taskforge.user.application.dto;

import com.kshitiz.taskforge.user.domain.model.Role;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for {@code PUT /api/users/{id}/role} (admins only).
 *
 * @param role the user's new role
 */
public record ChangeRoleCommand(@NotNull Role role) {
}
