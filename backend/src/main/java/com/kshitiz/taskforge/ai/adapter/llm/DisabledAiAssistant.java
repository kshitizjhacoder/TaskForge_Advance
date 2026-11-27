package com.kshitiz.taskforge.ai.adapter.llm;

import com.kshitiz.taskforge.ai.domain.model.BreakdownRequest;
import com.kshitiz.taskforge.ai.domain.model.SubtaskSuggestion;
import com.kshitiz.taskforge.ai.domain.port.AiAssistant;
import com.kshitiz.taskforge.common.exception.AiUnavailableException;

import java.util.List;

/**
 * {@link AiAssistant} used when AI is switched off ({@code AI_PROVIDER=none}, the default
 * outside development). The rest of the application works normally; AI endpoints answer 503
 * and the status endpoint reports {@code enabled: false} so the UI can hide AI buttons.
 */
public class DisabledAiAssistant implements AiAssistant {

    /**
     * Always refuses.
     *
     * @param request ignored
     * @return never returns
     * @throws AiUnavailableException always
     */
    @Override
    public List<SubtaskSuggestion> suggestSubtasks(BreakdownRequest request) {
        throw new AiUnavailableException("AI features are turned off on this server.");
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEnabled() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String providerName() {
        return "none";
    }
}
