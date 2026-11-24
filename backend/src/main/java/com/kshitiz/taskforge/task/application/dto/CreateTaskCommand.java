package com.kshitiz.taskforge.task.application.dto;

import com.kshitiz.taskforge.task.domain.model.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Request body for creating a task ({@code POST /api/tasks}).
 *
 * @param projectId   project the task belongs to (optional until Phase 3)
 * @param title       short summary, required
 * @param description details (optional)
 * @param createdBy   creator's user ID. Temporary: from Phase 2 this comes from the logged-in
 *                    user instead of the request body.
 * @param startDate   planned start (optional, ISO date such as 2026-12-01)
 * @param endDate     due date (optional, not before startDate)
 * @param priority    urgency; MEDIUM if omitted
 * @param tags        labels (optional, at most 10)
 */
public record CreateTaskCommand(
        UUID projectId,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 10_000) String description,
        @NotNull UUID createdBy,
        LocalDate startDate,
        LocalDate endDate,
        Priority priority,
        @Size(max = 10) List<String> tags) {
}
