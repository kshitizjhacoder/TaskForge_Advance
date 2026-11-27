package com.kshitiz.taskforge.ai.application;

import com.kshitiz.taskforge.ai.application.dto.AiStatusResponse;
import com.kshitiz.taskforge.ai.application.dto.BreakdownResponse;
import com.kshitiz.taskforge.ai.domain.model.BreakdownRequest;
import com.kshitiz.taskforge.ai.domain.model.SubtaskSuggestion;
import com.kshitiz.taskforge.ai.domain.port.AiAssistant;
import com.kshitiz.taskforge.common.exception.AiInvalidResponseException;
import com.kshitiz.taskforge.common.exception.AiQuotaExceededException;
import com.kshitiz.taskforge.common.exception.AiUnavailableException;
import com.kshitiz.taskforge.common.exception.BusinessRuleViolationException;
import com.kshitiz.taskforge.common.exception.ResourceNotFoundException;
import com.kshitiz.taskforge.common.security.CurrentUser;
import com.kshitiz.taskforge.project.application.ProjectService;
import com.kshitiz.taskforge.task.application.TaskService;
import com.kshitiz.taskforge.task.application.dto.TaskResponse;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Use case "Break down with AI": asks the AI to split a task into subtasks and returns
 * cleaned suggestions for the user to review.
 *
 * <p>Guardrails, in order:
 * <ol>
 *   <li>AI must be enabled, and the user must be able to see the task (project member);</li>
 *   <li>subtasks cannot be split further (one level only);</li>
 *   <li>daily limits per user and for the whole app ({@link AiQuotaService});</li>
 *   <li>only task text is sent, never IDs or other users' data; long descriptions are cut;</li>
 *   <li>the answer is cleaned ({@link SuggestionSanitizer}); if nothing usable comes back the
 *       model is asked once more, then the request fails with 502;</li>
 *   <li>nothing is saved: the user decides which suggestions to keep.</li>
 * </ol>
 * Outcomes are counted in the metric {@code taskforge.ai.breakdown} (by outcome tag), and the
 * task text is never logged.
 *
 * <p>Deliberately not {@code @Transactional}: no database connection is held while waiting
 * for the AI, which can take seconds.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskBreakdownService {

    /** Longest description sent to the AI, to bound prompt size and cost. */
    static final int MAX_DESCRIPTION_CHARS = 4000;
    private static final int MAX_ATTEMPTS = 2;
    private static final String METRIC = "taskforge.ai.breakdown";

    private final TaskService taskService;
    private final ProjectService projectService;
    private final AiAssistant aiAssistant;
    private final AiQuotaService quotaService;
    private final AiProperties properties;
    private final MeterRegistry meterRegistry;

    /**
     * Suggests subtasks for a task.
     *
     * @param taskId task to split up
     * @param actor  the logged-in user
     * @return cleaned suggestions and the user's remaining AI requests
     * @throws AiUnavailableException         if AI is off or the provider fails (503)
     * @throws ResourceNotFoundException      if the task is not found or not visible (404)
     * @throws BusinessRuleViolationException if the task is itself a subtask (422)
     * @throws AiQuotaExceededException       if a daily limit is reached (429)
     * @throws AiInvalidResponseException     if the AI gives no usable answer twice (502)
     */
    public BreakdownResponse suggestBreakdown(UUID taskId, CurrentUser actor) {
        if (!aiAssistant.isEnabled()) {
            throw new AiUnavailableException("AI features are turned off on this server.");
        }
        TaskResponse task = taskService.getTask(taskId, actor);
        if (task.parentTaskId() != null) {
            throw new BusinessRuleViolationException("Subtasks cannot be broken down further");
        }
        String projectName = projectService.getProject(task.projectId(), actor).name();
        quotaService.reserve(actor.id());

        BreakdownRequest request = new BreakdownRequest(projectName, task.title(),
                shorten(task.description()), task.tags(), properties.maxSuggestions());
        try {
            List<SubtaskSuggestion> suggestions = askWithOneRetry(request, task.title());
            record("success");
            log.info("AI breakdown for task {} by user {}: {} suggestions", taskId, actor.id(), suggestions.size());
            return new BreakdownResponse(taskId, suggestions, quotaService.remainingToday(actor.id()));
        } catch (AiUnavailableException ex) {
            record("unavailable");
            throw ex;
        } catch (AiInvalidResponseException ex) {
            record("invalid");
            throw ex;
        }
    }

    /**
     * Reports whether AI is available and how many requests the user has left.
     *
     * @param actor the logged-in user
     * @return AI status for the UI
     */
    public AiStatusResponse status(CurrentUser actor) {
        return new AiStatusResponse(aiAssistant.isEnabled(), aiAssistant.providerName(),
                properties.userDailyLimit(), quotaService.remainingToday(actor.id()));
    }

    /**
     * Calls the AI and cleans the answer; if nothing usable comes back, tries once more.
     *
     * @param request     what to send to the AI
     * @param parentTitle title of the task, to drop suggestions that just repeat it
     * @return at least one clean suggestion
     * @throws AiInvalidResponseException if both attempts give nothing usable
     */
    private List<SubtaskSuggestion> askWithOneRetry(BreakdownRequest request, String parentTitle) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                List<SubtaskSuggestion> clean = SuggestionSanitizer.sanitize(
                        aiAssistant.suggestSubtasks(request), parentTitle, properties.maxSuggestions());
                if (!clean.isEmpty()) {
                    return clean;
                }
                log.warn("AI returned no usable suggestions (attempt {})", attempt);
            } catch (AiInvalidResponseException ex) {
                log.warn("AI returned an unreadable answer (attempt {})", attempt);
            }
        }
        throw new AiInvalidResponseException(
                "The AI could not suggest useful subtasks for this task. Try adding more detail to the description.");
    }

    /**
     * Shortens a description to {@value #MAX_DESCRIPTION_CHARS} characters.
     *
     * @param description task description, may be {@code null}
     * @return the description, shortened if needed
     */
    private static String shorten(String description) {
        if (description == null || description.length() <= MAX_DESCRIPTION_CHARS) {
            return description;
        }
        return description.substring(0, MAX_DESCRIPTION_CHARS);
    }

    /**
     * Counts one breakdown request in the metrics, tagged with its outcome and provider.
     *
     * @param outcome "success", "invalid" or "unavailable"
     */
    private void record(String outcome) {
        meterRegistry.counter(METRIC, "outcome", outcome, "provider", aiAssistant.providerName()).increment();
    }
}
