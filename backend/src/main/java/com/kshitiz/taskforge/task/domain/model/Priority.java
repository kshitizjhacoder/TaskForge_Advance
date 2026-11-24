package com.kshitiz.taskforge.task.domain.model;

/**
 * How urgent a task is.
 *
 * <p>The codes must match the rows of the {@code master_priority} table, which holds the
 * labels and colours shown in the UI. The application refuses to start if they differ.
 */
public enum Priority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
