package com.kshitiz.taskforge.ai.domain.model;

import java.util.List;

/**
 * What the AI is told about the task it should split up. Only these fields are sent to the
 * AI provider: no IDs, emails or other users' data.
 *
 * @param projectName     name of the task's project, for context
 * @param taskTitle       the task's title
 * @param taskDescription the task's description (may be {@code null}; already shortened)
 * @param tags            the task's tags
 * @param maxSuggestions  most subtasks the AI may propose
 */
public record BreakdownRequest(String projectName, String taskTitle, String taskDescription, List<String> tags,
                               int maxSuggestions) {
}
