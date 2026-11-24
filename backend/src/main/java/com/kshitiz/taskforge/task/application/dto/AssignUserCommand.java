package com.kshitiz.taskforge.task.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request body for {@code POST /api/tasks/{taskId}/assignees}.
 *
 * @param userId the user to assign to the task
 */
public record AssignUserCommand(@NotNull UUID userId) {
}
