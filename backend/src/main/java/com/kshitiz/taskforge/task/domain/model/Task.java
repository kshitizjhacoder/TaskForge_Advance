package com.kshitiz.taskforge.task.domain.model;

import com.kshitiz.taskforge.common.exception.BusinessRuleViolationException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A unit of work: what needs doing, how urgent it is, its dates and where it is in the
 * workflow. This class owns the task rules, so they hold no matter which API or service
 * changes a task:
 * <ul>
 *   <li>a title is required (max {@value #MAX_TITLE_LENGTH} characters);</li>
 *   <li>the end date cannot be before the start date;</li>
 *   <li>status changes must follow {@link TaskStatus#allowedNextStatuses()};</li>
 *   <li>tags are trimmed, lower-cased and de-duplicated (max {@value #MAX_TAGS});</li>
 *   <li>a task may have subtasks, one level deep: a subtask lives in its parent's project and
 *       cannot have subtasks of its own.</li>
 * </ul>
 * New tasks are made with {@link #create} or {@link #createSubtask}; tasks loaded from the
 * database are rebuilt with {@code Task.restore()...build()} (used only by the persistence
 * mapper).
 */
@Getter
@Builder(builderMethodName = "restore")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Task {

    public static final int MAX_TITLE_LENGTH = 200;
    public static final int MAX_TAGS = 10;
    public static final int MAX_TAG_LENGTH = 50;

    private final UUID id;
    /** Project the task belongs to (required; never changes). */
    private final UUID projectId;
    /** The parent task if this is a subtask, otherwise {@code null}. */
    private final UUID parentTaskId;
    private String title;
    private String description;
    private final UUID createdBy;
    private LocalDate startDate;
    private LocalDate endDate;
    private Priority priority;
    private TaskStatus status;
    private List<String> tags;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Long version;

    /**
     * Creates a new task in status {@link TaskStatus#TO_DO}.
     *
     * @param projectId   project the task belongs to (required)
     * @param title       short summary of the work
     * @param description details (optional)
     * @param createdBy   ID of the user creating the task
     * @param startDate   planned start (optional)
     * @param endDate     due date (optional)
     * @param priority    urgency; MEDIUM if {@code null}
     * @param tags        labels (optional)
     * @return the new, not yet saved, task
     * @throws BusinessRuleViolationException if any rule listed on the class is broken
     */
    public static Task create(UUID projectId, String title, String description, UUID createdBy,
                              LocalDate startDate, LocalDate endDate, Priority priority, List<String> tags) {
        Objects.requireNonNull(createdBy, "createdBy");
        if (projectId == null) {
            throw new BusinessRuleViolationException("A task must belong to a project");
        }
        validateDates(startDate, endDate);
        return new Task(UUID.randomUUID(), projectId, null, validateTitle(title), trimToNull(description),
                createdBy, startDate, endDate, priority == null ? Priority.MEDIUM : priority, TaskStatus.TO_DO,
                normalizeTags(tags), null, null, null);
    }

    /**
     * Creates a subtask of {@code parent}, in the parent's project, in status TO_DO.
     *
     * @param parent      the task being split up
     * @param title       short summary of the step
     * @param description details (optional)
     * @param createdBy   ID of the user creating the subtask
     * @param startDate   planned start (optional)
     * @param endDate     due date (optional)
     * @param priority    urgency; MEDIUM if {@code null}
     * @param tags        labels (optional)
     * @return the new, not yet saved, subtask
     * @throws BusinessRuleViolationException if the parent is itself a subtask or a rule is broken
     */
    public static Task createSubtask(Task parent, String title, String description, UUID createdBy,
                                     LocalDate startDate, LocalDate endDate, Priority priority, List<String> tags) {
        Objects.requireNonNull(parent, "parent");
        Objects.requireNonNull(createdBy, "createdBy");
        if (parent.isSubtask()) {
            throw new BusinessRuleViolationException("Subtasks cannot have subtasks of their own");
        }
        validateDates(startDate, endDate);
        return new Task(UUID.randomUUID(), parent.getProjectId(), parent.getId(), validateTitle(title),
                trimToNull(description), createdBy, startDate, endDate,
                priority == null ? Priority.MEDIUM : priority, TaskStatus.TO_DO, normalizeTags(tags),
                null, null, null);
    }

    /**
     * Checks whether this task is a subtask of another task.
     *
     * @return {@code true} if it has a parent
     */
    public boolean isSubtask() {
        return parentTaskId != null;
    }

    /**
     * Replaces the editable details of the task. Status is changed separately with
     * {@link #changeStatus} because it follows its own rules.
     *
     * @param title       new title
     * @param description new description (optional)
     * @param startDate   new start date (optional)
     * @param endDate     new due date (optional)
     * @param priority    new priority
     * @param tags        new labels (optional)
     * @throws BusinessRuleViolationException if any rule listed on the class is broken
     */
    public void updateDetails(String title, String description, LocalDate startDate, LocalDate endDate,
                              Priority priority, List<String> tags) {
        validateDates(startDate, endDate);
        if (priority == null) {
            throw new BusinessRuleViolationException("Priority is required");
        }
        this.title = validateTitle(title);
        this.description = trimToNull(description);
        this.startDate = startDate;
        this.endDate = endDate;
        this.priority = priority;
        this.tags = normalizeTags(tags);
    }

    /**
     * Moves the task to a new status. Asking for the current status does nothing.
     *
     * @param newStatus requested status
     * @throws BusinessRuleViolationException if the workflow does not allow this move
     */
    public void changeStatus(TaskStatus newStatus) {
        Objects.requireNonNull(newStatus, "newStatus");
        if (newStatus == status) {
            return;
        }
        if (!status.canMoveTo(newStatus)) {
            throw new BusinessRuleViolationException("Cannot move a task from " + status + " to " + newStatus
                    + ". Allowed next statuses: " + status.allowedNextStatuses());
        }
        this.status = newStatus;
    }

    /**
     * Checks and cleans a title.
     *
     * @param title raw title
     * @return trimmed title
     */
    private static String validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new BusinessRuleViolationException("Title is required");
        }
        String trimmed = title.trim();
        if (trimmed.length() > MAX_TITLE_LENGTH) {
            throw new BusinessRuleViolationException("Title must be at most " + MAX_TITLE_LENGTH + " characters");
        }
        return trimmed;
    }

    /**
     * Checks that the end date is not before the start date (when both are set).
     *
     * @param startDate planned start
     * @param endDate   due date
     */
    private static void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessRuleViolationException("End date cannot be before start date");
        }
    }

    /**
     * Cleans tags: trims, lower-cases, drops blanks and duplicates, keeps the original order.
     *
     * @param tags raw tags, may be {@code null}
     * @return cleaned, read-only list (never {@code null})
     */
    private static List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return List.of();
        }
        Set<String> cleaned = new LinkedHashSet<>();
        for (String tag : tags) {
            if (tag == null || tag.isBlank()) {
                continue;
            }
            String value = tag.trim().toLowerCase(Locale.ROOT);
            if (value.length() > MAX_TAG_LENGTH) {
                throw new BusinessRuleViolationException("Tags must be at most " + MAX_TAG_LENGTH + " characters");
            }
            cleaned.add(value);
        }
        if (cleaned.size() > MAX_TAGS) {
            throw new BusinessRuleViolationException("A task can have at most " + MAX_TAGS + " tags");
        }
        return List.copyOf(cleaned);
    }

    /**
     * Trims text and turns blank text into {@code null}.
     *
     * @param text raw text
     * @return trimmed text, or {@code null} if blank
     */
    private static String trimToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
