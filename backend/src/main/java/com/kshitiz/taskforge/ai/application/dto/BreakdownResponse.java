package com.kshitiz.taskforge.ai.application.dto;

import com.kshitiz.taskforge.ai.domain.model.SubtaskSuggestion;

import java.util.List;
import java.util.UUID;

/**
 * Response of {@code POST /api/tasks/{id}/ai/breakdown}: suggested subtasks for the user to
 * review. Nothing is saved; the user sends the ones they keep to
 * {@code POST /api/tasks/{id}/subtasks}.
 *
 * @param taskId         the task that was broken down
 * @param suggestions    cleaned suggestions
 * @param remainingToday AI requests the user has left today
 */
public record BreakdownResponse(UUID taskId, List<SubtaskSuggestion> suggestions, int remainingToday) {
}
