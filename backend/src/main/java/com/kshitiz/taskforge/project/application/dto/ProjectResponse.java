package com.kshitiz.taskforge.project.application.dto;

import com.kshitiz.taskforge.project.domain.model.Project;

import java.time.Instant;
import java.util.UUID;

/**
 * JSON shape of a project returned by the API.
 *
 * @param id          project ID
 * @param key         short identifier, e.g. "TF"
 * @param name        display name
 * @param description details, may be {@code null}
 * @param ownerId     the owner's user ID
 * @param createdAt   when the project was created
 * @param updatedAt   when it was last changed
 */
public record ProjectResponse(UUID id, String key, String name, String description, UUID ownerId,
                              Instant createdAt, Instant updatedAt) {

    /**
     * Converts a domain project into its API representation.
     *
     * @param project domain object
     * @return response DTO
     */
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(project.getId(), project.getKey(), project.getName(), project.getDescription(),
                project.getOwnerId(), project.getCreatedAt(), project.getUpdatedAt());
    }
}
