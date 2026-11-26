package com.kshitiz.taskforge.project.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code PUT /api/projects/{id}}. The key cannot be changed.
 *
 * @param name        new display name
 * @param description new details (optional; omitted clears it)
 */
public record UpdateProjectCommand(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 10_000) String description) {
}
