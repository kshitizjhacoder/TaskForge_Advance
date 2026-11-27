package com.kshitiz.taskforge.task.application.dto;

import com.kshitiz.taskforge.task.domain.model.Priority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request body for {@code POST /api/tasks/{id}/subtasks}: creates several subtasks at once,
 * typically the AI suggestions the user chose to keep.
 *
 * @param subtasks the subtasks to create (1–10)
 */
public record CreateSubtasksCommand(@NotEmpty @Size(max = 10) List<@Valid Item> subtasks) {

    /**
     * One subtask to create.
     *
     * @param title       short summary, required
     * @param description details (optional)
     * @param priority    urgency; MEDIUM if omitted
     */
    public record Item(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description,
            Priority priority) {
    }
}
