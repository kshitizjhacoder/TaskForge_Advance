package com.kshitiz.taskforge.task.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Records that a user is assigned to a task. A task can have several assignees.
 *
 * @param taskId     the task
 * @param userId     the assigned user
 * @param assignedAt when the assignment was made
 */
public record TaskAssignment(UUID taskId, UUID userId, Instant assignedAt) {

    /**
     * Creates an assignment stamped with the current time.
     *
     * @param taskId the task
     * @param userId the user to assign
     * @return the new assignment
     */
    public static TaskAssignment of(UUID taskId, UUID userId) {
        return new TaskAssignment(Objects.requireNonNull(taskId, "taskId"),
                Objects.requireNonNull(userId, "userId"), Instant.now());
    }
}
