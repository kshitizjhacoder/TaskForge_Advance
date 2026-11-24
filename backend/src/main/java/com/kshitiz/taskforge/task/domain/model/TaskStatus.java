package com.kshitiz.taskforge.task.domain.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Where a task is in its workflow, and which moves between statuses are allowed.
 *
 * <pre>
 *   From         Allowed next statuses
 *   TO_DO        IN_PROGRESS, BLOCKED
 *   IN_PROGRESS  TO_DO, BLOCKED, DONE
 *   BLOCKED      TO_DO, IN_PROGRESS
 *   DONE         IN_PROGRESS  (reopen)
 * </pre>
 *
 * <p>A task cannot jump from TO_DO straight to DONE: work has to start first. A finished
 * task can be reopened (back to IN_PROGRESS).
 *
 * <p>The codes must match the rows of the {@code master_task_status} table.
 */
public enum TaskStatus {
    TO_DO,
    IN_PROGRESS,
    BLOCKED,
    DONE;

    /**
     * Lists the statuses a task in this status may move to next.
     *
     * @return allowed next statuses (read-only)
     */
    public Set<TaskStatus> allowedNextStatuses() {
        return switch (this) {
            case TO_DO -> Collections.unmodifiableSet(EnumSet.of(IN_PROGRESS, BLOCKED));
            case IN_PROGRESS -> Collections.unmodifiableSet(EnumSet.of(TO_DO, BLOCKED, DONE));
            case BLOCKED -> Collections.unmodifiableSet(EnumSet.of(TO_DO, IN_PROGRESS));
            case DONE -> Collections.unmodifiableSet(EnumSet.of(IN_PROGRESS));
        };
    }

    /**
     * Checks whether a task in this status may move to {@code next}.
     *
     * @param next the requested new status
     * @return {@code true} if the move is allowed
     */
    public boolean canMoveTo(TaskStatus next) {
        return allowedNextStatuses().contains(next);
    }
}
