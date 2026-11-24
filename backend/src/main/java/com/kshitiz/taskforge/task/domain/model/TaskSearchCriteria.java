package com.kshitiz.taskforge.task.domain.model;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Optional filters for searching tasks. A {@code null} field means "don't filter on this";
 * all set filters must match (AND).
 *
 * @param text       text contained in the title, case-insensitive
 * @param status     exact status
 * @param priority   exact priority
 * @param projectId  project the task belongs to
 * @param createdBy  creator's user ID
 * @param assigneeId a user who is assigned to the task
 * @param dueFrom    end date on or after this day
 * @param dueTo      end date on or before this day
 */
public record TaskSearchCriteria(String text, TaskStatus status, Priority priority, UUID projectId,
                                 UUID createdBy, UUID assigneeId, LocalDate dueFrom, LocalDate dueTo) {
}
