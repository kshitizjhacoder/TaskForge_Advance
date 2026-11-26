package com.kshitiz.taskforge.project.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/projects} (managers and admins). The logged-in user
 * becomes the owner.
 *
 * @param key         2–10 letters/digits starting with a letter, e.g. "TF" (case-insensitive)
 * @param name        display name
 * @param description details (optional)
 */
public record CreateProjectCommand(
        @NotBlank @Pattern(regexp = "[A-Za-z][A-Za-z0-9]{1,9}",
                message = "must be 2-10 letters or digits, starting with a letter") String key,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 10_000) String description) {
}
