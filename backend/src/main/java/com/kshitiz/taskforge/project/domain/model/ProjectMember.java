package com.kshitiz.taskforge.project.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Records that a user belongs to a project.
 *
 * @param projectId the project
 * @param userId    the member
 * @param joinedAt  when they were added
 */
public record ProjectMember(UUID projectId, UUID userId, Instant joinedAt) {

    /**
     * Creates a membership stamped with the current time.
     *
     * @param projectId the project
     * @param userId    the user to add
     * @return the new membership
     */
    public static ProjectMember of(UUID projectId, UUID userId) {
        return new ProjectMember(Objects.requireNonNull(projectId, "projectId"),
                Objects.requireNonNull(userId, "userId"), Instant.now());
    }
}
