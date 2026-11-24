package com.kshitiz.taskforge.task.application.dto;

import com.kshitiz.taskforge.task.domain.model.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Request body for editing a task's details ({@code PUT /api/tasks/{id}}). Every field is
 * replaced, so send the full set; omitted optional fields are cleared. Status is changed with
 * its own endpoint.
 *
 * @param title       short summary, required
 * @param description details (optional)
 * @param startDate   planned start (optional)
 * @param endDate     due date (optional, not before startDate)
 * @param priority    urgency, required
 * @param tags        labels (optional, at most 10)
 */
public record UpdateTaskCommand(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 10_000) String description,
        LocalDate startDate,
        LocalDate endDate,
        @NotNull Priority priority,
        @Size(max = 10) List<String> tags) {
}
