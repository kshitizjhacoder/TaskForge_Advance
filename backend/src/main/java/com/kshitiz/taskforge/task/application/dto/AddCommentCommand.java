package com.kshitiz.taskforge.task.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request body for {@code POST /api/tasks/{taskId}/comments}.
 *
 * @param authorId user writing the comment. Temporary: from Phase 2 this comes from the
 *                 logged-in user.
 * @param content  comment text (max 5000 characters)
 */
public record AddCommentCommand(@NotNull UUID authorId, @NotBlank @Size(max = 5000) String content) {
}
