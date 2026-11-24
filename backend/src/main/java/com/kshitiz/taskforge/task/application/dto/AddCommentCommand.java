package com.kshitiz.taskforge.task.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/tasks/{taskId}/comments}. The author is the logged-in
 * user, taken from the access token.
 *
 * @param content comment text (max 5000 characters)
 */
public record AddCommentCommand(@NotBlank @Size(max = 5000) String content) {
}
