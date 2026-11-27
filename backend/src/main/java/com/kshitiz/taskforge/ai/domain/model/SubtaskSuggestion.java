package com.kshitiz.taskforge.ai.domain.model;

import com.kshitiz.taskforge.task.domain.model.Priority;

/**
 * One subtask proposed by the AI. It is only a suggestion: nothing is saved until the user
 * accepts it (through {@code POST /api/tasks/{id}/subtasks}).
 *
 * @param title       short, action-style summary
 * @param description one or two sentences of detail (may be {@code null})
 * @param priority    suggested urgency (may be {@code null} if the AI gave an unknown value)
 */
public record SubtaskSuggestion(String title, String description, Priority priority) {
}
