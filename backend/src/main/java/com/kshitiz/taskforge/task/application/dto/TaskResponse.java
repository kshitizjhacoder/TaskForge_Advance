package com.kshitiz.taskforge.task.application.dto;

import com.kshitiz.taskforge.task.domain.model.Priority;
import com.kshitiz.taskforge.task.domain.model.Task;
import com.kshitiz.taskforge.task.domain.model.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * JSON shape of a task returned by the API.
 *
 * @param id           task ID
 * @param projectId    project ID
 * @param parentTaskId parent task for subtasks, {@code null} otherwise
 * @param title        short summary
 * @param description  details, may be {@code null}
 * @param createdBy    creator's user ID
 * @param startDate    planned start, may be {@code null}
 * @param endDate      due date, may be {@code null}
 * @param priority     urgency
 * @param status       workflow status
 * @param tags         labels
 * @param createdAt    when the task was created
 * @param updatedAt    when the task was last changed
 * @param version      edit counter, used to detect conflicting edits
 */
public record TaskResponse(UUID id, UUID projectId, UUID parentTaskId, String title, String description,
                           UUID createdBy, LocalDate startDate, LocalDate endDate, Priority priority,
                           TaskStatus status, List<String> tags, Instant createdAt, Instant updatedAt,
                           Long version) {

    /**
     * Converts a domain task into its API representation.
     *
     * @param task domain object
     * @return response DTO
     */
    public static TaskResponse from(Task task) {
        return new TaskResponse(task.getId(), task.getProjectId(), task.getParentTaskId(), task.getTitle(),
                task.getDescription(), task.getCreatedBy(), task.getStartDate(), task.getEndDate(),
                task.getPriority(), task.getStatus(), task.getTags(), task.getCreatedAt(), task.getUpdatedAt(),
                task.getVersion());
    }
}
