package com.kshitiz.taskforge.user.application.dto;

import java.util.UUID;

/**
 * Request body for {@code PUT /api/users/{id}/manager}.
 *
 * @param managerId the new manager's ID, or {@code null} to remove the current manager
 */
public record AssignManagerCommand(UUID managerId) {
}
