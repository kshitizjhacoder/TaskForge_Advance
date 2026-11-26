package com.kshitiz.taskforge.project.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request body for {@code POST /api/projects/{id}/members}.
 *
 * @param userId the user to add to the project
 */
public record AddMemberCommand(@NotNull UUID userId) {
}
