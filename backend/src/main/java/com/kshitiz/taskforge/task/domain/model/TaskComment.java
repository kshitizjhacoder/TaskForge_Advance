package com.kshitiz.taskforge.task.domain.model;

import com.kshitiz.taskforge.common.exception.BusinessRuleViolationException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A comment written by a user on a task.
 *
 * <p>New comments are made with {@link #write}; comments loaded from the database are rebuilt
 * with {@code TaskComment.restore()...build()}.
 */
@Getter
@Builder(builderMethodName = "restore")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TaskComment {

    public static final int MAX_CONTENT_LENGTH = 5000;

    private final UUID id;
    private final UUID taskId;
    private final UUID authorId;
    private String content;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Long version;

    /**
     * Creates a new comment.
     *
     * @param taskId   task being commented on
     * @param authorId user writing the comment
     * @param content  comment text
     * @return the new, not yet saved, comment
     * @throws BusinessRuleViolationException if the text is empty or too long
     */
    public static TaskComment write(UUID taskId, UUID authorId, String content) {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(authorId, "authorId");
        return new TaskComment(UUID.randomUUID(), taskId, authorId, validateContent(content), null, null, null);
    }

    /**
     * Replaces the comment text.
     *
     * @param newContent new text
     * @throws BusinessRuleViolationException if the text is empty or too long
     */
    public void edit(String newContent) {
        this.content = validateContent(newContent);
    }

    /**
     * Checks and trims comment text.
     *
     * @param content raw text
     * @return trimmed text
     */
    private static String validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessRuleViolationException("Comment cannot be empty");
        }
        String trimmed = content.trim();
        if (trimmed.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessRuleViolationException("Comment must be at most " + MAX_CONTENT_LENGTH + " characters");
        }
        return trimmed;
    }
}
