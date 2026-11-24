package com.kshitiz.taskforge.task.application.dto;

import com.kshitiz.taskforge.task.domain.model.TaskComment;

import java.time.Instant;
import java.util.UUID;

/**
 * JSON shape of a task comment returned by the API.
 *
 * @param id        comment ID
 * @param taskId    task the comment belongs to
 * @param authorId  user who wrote it
 * @param content   comment text
 * @param createdAt when it was written
 * @param updatedAt when it was last edited
 */
public record CommentResponse(UUID id, UUID taskId, UUID authorId, String content,
                              Instant createdAt, Instant updatedAt) {

    /**
     * Converts a domain comment into its API representation.
     *
     * @param comment domain object
     * @return response DTO
     */
    public static CommentResponse from(TaskComment comment) {
        return new CommentResponse(comment.getId(), comment.getTaskId(), comment.getAuthorId(),
                comment.getContent(), comment.getCreatedAt(), comment.getUpdatedAt());
    }
}
