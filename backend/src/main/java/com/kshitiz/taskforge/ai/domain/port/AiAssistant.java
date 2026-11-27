package com.kshitiz.taskforge.ai.domain.port;

import com.kshitiz.taskforge.ai.domain.model.BreakdownRequest;
import com.kshitiz.taskforge.ai.domain.model.SubtaskSuggestion;
import com.kshitiz.taskforge.common.exception.AiInvalidResponseException;
import com.kshitiz.taskforge.common.exception.AiUnavailableException;

import java.util.List;

/**
 * Port (interface) for the AI model. TaskForge's services only talk to this interface, so the
 * provider can be swapped by configuration ({@code AI_PROVIDER}) without touching them:
 * <ul>
 *   <li>{@code GeminiAiAssistant} – Google Gemini through Spring AI;</li>
 *   <li>{@code FakeAiAssistant} – fixed answers for development and tests;</li>
 *   <li>{@code DisabledAiAssistant} – AI switched off.</li>
 * </ul>
 * The AI only <em>suggests</em>; it has no access to the database and no tools, so it cannot
 * change anything by itself.
 */
public interface AiAssistant {

    /**
     * Asks the model to split a task into smaller steps.
     *
     * @param request what to tell the model about the task
     * @return the model's suggestions, not yet checked or cleaned
     * @throws AiUnavailableException      if the provider is off, unreachable, too slow or rate-limited
     * @throws AiInvalidResponseException  if the answer cannot be read
     */
    List<SubtaskSuggestion> suggestSubtasks(BreakdownRequest request);

    /**
     * Tells whether AI features are available, so the UI can hide the button when not.
     *
     * @return {@code false} when AI is switched off
     */
    boolean isEnabled();

    /**
     * Short name of the provider, shown in the AI status endpoint and used in logs/metrics.
     *
     * @return e.g. "google-genai", "fake" or "none"
     */
    String providerName();
}
