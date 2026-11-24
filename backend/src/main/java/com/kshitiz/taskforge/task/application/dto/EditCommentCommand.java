package com.kshitiz.taskforge.task.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code PUT /api/comments/{id}}.
 *
 * @param content new comment text (max 5000 characters)
 */
public record EditCommentCommand(@NotBlank @Size(max = 5000) String content) {
}
