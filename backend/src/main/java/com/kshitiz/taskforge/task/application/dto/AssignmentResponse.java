package com.kshitiz.taskforge.task.application.dto;

import com.kshitiz.taskforge.task.domain.model.TaskAssignment;

import java.time.Instant;
import java.util.UUID;

/**
 * JSON shape of a task assignment returned by the API.
 *
 * @param taskId     the task
 * @param userId     the assigned user
 * @param assignedAt when the assignment was made
 */
public record AssignmentResponse(UUID taskId, UUID userId, Instant assignedAt) {

    /**
     * Converts a domain assignment into its API representation.
     *
     * @param assignment domain object
     * @return response DTO
     */
    public static AssignmentResponse from(TaskAssignment assignment) {
        return new AssignmentResponse(assignment.taskId(), assignment.userId(), assignment.assignedAt());
    }
}
