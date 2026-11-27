package com.kshitiz.taskforge.task.application.dto;

import com.kshitiz.taskforge.task.domain.model.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Request body for creating a task ({@code POST /api/tasks}). The creator is the logged-in
 * user, taken from the access token.
 *
 * @param projectId    project the task belongs to (required; the user must be a member)
 * @param parentTaskId parent task, to create a subtask (optional; must be in the same project)
 * @param title        short summary, required
 * @param description  details (optional)
 * @param startDate    planned start (optional, ISO date such as 2026-12-01)
 * @param endDate      due date (optional, not before startDate)
 * @param priority     urgency; MEDIUM if omitted
 * @param tags         labels (optional, at most 10)
 */
public record CreateTaskCommand(
        @NotNull UUID projectId,
        UUID parentTaskId,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 10_000) String description,
        LocalDate startDate,
        LocalDate endDate,
        Priority priority,
        @Size(max = 10) List<String> tags) {
}
