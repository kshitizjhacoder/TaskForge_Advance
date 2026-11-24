package com.kshitiz.taskforge.task.domain.port;

import com.kshitiz.taskforge.task.domain.model.TaskComment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port (interface) for storing and loading task comments. Implemented by
 * {@code TaskCommentPersistenceAdapter}. Deleted comments are invisible to every method.
 */
public interface TaskCommentRepository {

    /**
     * Inserts a new comment or updates an existing one.
     *
     * @param comment comment to store
     * @return the stored comment with timestamps and version filled in
     */
    TaskComment save(TaskComment comment);

    /**
     * Finds a comment by ID.
     *
     * @param id comment ID
     * @return the comment, or empty if not found
     */
    Optional<TaskComment> findById(UUID id);

    /**
     * Lists all comments on a task, oldest first (reading order).
     *
     * @param taskId task ID
     * @return comments
     */
    List<TaskComment> findByTaskId(UUID taskId);

    /**
     * Soft-deletes a comment.
     *
     * @param id comment ID
     */
    void deleteById(UUID id);
}
