package com.kshitiz.taskforge.project.application.dto;

import com.kshitiz.taskforge.project.domain.model.ProjectMember;

import java.time.Instant;
import java.util.UUID;

/**
 * JSON shape of a project membership.
 *
 * @param projectId the project
 * @param userId    the member
 * @param owner     {@code true} for the project owner
 * @param joinedAt  when they were added
 */
public record ProjectMemberResponse(UUID projectId, UUID userId, boolean owner, Instant joinedAt) {

    /**
     * Converts a domain membership into its API representation.
     *
     * @param member  domain membership
     * @param ownerId the project's owner, to set the {@code owner} flag
     * @return response DTO
     */
    public static ProjectMemberResponse from(ProjectMember member, UUID ownerId) {
        return new ProjectMemberResponse(member.projectId(), member.userId(), member.userId().equals(ownerId),
                member.joinedAt());
    }
}
