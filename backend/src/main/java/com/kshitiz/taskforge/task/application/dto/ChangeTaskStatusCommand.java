package com.kshitiz.taskforge.task.application.dto;

import com.kshitiz.taskforge.task.domain.model.TaskStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for {@code PATCH /api/tasks/{id}/status}.
 *
 * @param status the status to move the task to
 */
public record ChangeTaskStatusCommand(@NotNull TaskStatus status) {
}
