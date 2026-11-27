package com.kshitiz.taskforge.task.domain.model;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Optional filters for searching tasks. A {@code null} field means "don't filter on this";
 * all set filters must match (AND).
 *
 * @param text              text contained in the title, case-insensitive
 * @param status            exact status
 * @param priority          exact priority
 * @param projectId         project the task belongs to
 * @param parentTaskId      only subtasks of this task
 * @param createdBy         creator's user ID
 * @param assigneeId        a user who is assigned to the task
 * @param dueFrom           end date on or after this day
 * @param dueTo             end date on or before this day
 * @param visibleProjectIds only tasks in these projects; {@code null} means no limit. Set by
 *                          the service from the user's memberships, never by the client.
 */
public record TaskSearchCriteria(String text, TaskStatus status, Priority priority, UUID projectId,
                                 UUID parentTaskId, UUID createdBy, UUID assigneeId, LocalDate dueFrom,
                                 LocalDate dueTo, List<UUID> visibleProjectIds) {

    /**
     * Creates criteria from client filters, without any project visibility limit yet.
     *
     * @param text         title text
     * @param status       status
     * @param priority     priority
     * @param projectId    project
     * @param parentTaskId parent task (to list its subtasks)
     * @param createdBy    creator
     * @param assigneeId   assignee
     * @param dueFrom      earliest due date
     * @param dueTo        latest due date
     * @return the criteria
     */
    public static TaskSearchCriteria of(String text, TaskStatus status, Priority priority, UUID projectId,
                                        UUID parentTaskId, UUID createdBy, UUID assigneeId, LocalDate dueFrom,
                                        LocalDate dueTo) {
        return new TaskSearchCriteria(text, status, priority, projectId, parentTaskId, createdBy, assigneeId,
                dueFrom, dueTo, null);
    }

    /**
     * Returns a copy limited to the given projects.
     *
     * @param projectIds projects the user may see, or {@code null} for no limit (admins)
     * @return new criteria with the limit applied
     */
    public TaskSearchCriteria limitedTo(List<UUID> projectIds) {
        return new TaskSearchCriteria(text, status, priority, projectId, parentTaskId, createdBy, assigneeId,
                dueFrom, dueTo, projectIds);
    }
}
