package com.kshitiz.taskforge.ai.adapter.llm;

import com.kshitiz.taskforge.ai.domain.model.BreakdownRequest;
import com.kshitiz.taskforge.ai.domain.model.SubtaskSuggestion;
import com.kshitiz.taskforge.ai.domain.port.AiAssistant;
import com.kshitiz.taskforge.task.domain.model.Priority;

import java.util.List;

/**
 * {@link AiAssistant} that returns fixed, predictable suggestions without calling any
 * external service. Used in development (no API key needed) and in tests (fast, free,
 * repeatable). Enabled with {@code AI_PROVIDER=fake}.
 */
public class FakeAiAssistant implements AiAssistant {

    /**
     * Returns four generic steps built from the task title.
     *
     * @param request task data
     * @return four suggestions, or fewer if {@code maxSuggestions} is lower
     */
    @Override
    public List<SubtaskSuggestion> suggestSubtasks(BreakdownRequest request) {
        String title = request.taskTitle();
        List<SubtaskSuggestion> steps = List.of(
                new SubtaskSuggestion("Clarify requirements for: " + title,
                        "List what done looks like and any open questions.", Priority.HIGH),
                new SubtaskSuggestion("Implement: " + title, "Build the main change.", Priority.HIGH),
                new SubtaskSuggestion("Test: " + title, "Add or update automated tests.", Priority.MEDIUM),
                new SubtaskSuggestion("Review and release: " + title,
                        "Get a code review and ship the change.", Priority.LOW));
        return steps.subList(0, Math.min(steps.size(), request.maxSuggestions()));
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEnabled() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public String providerName() {
        return "fake";
    }
}
