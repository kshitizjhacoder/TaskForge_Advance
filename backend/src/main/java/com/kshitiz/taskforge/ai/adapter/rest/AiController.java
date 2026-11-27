package com.kshitiz.taskforge.ai.adapter.rest;

import com.kshitiz.taskforge.ai.application.TaskBreakdownService;
import com.kshitiz.taskforge.ai.application.dto.AiStatusResponse;
import com.kshitiz.taskforge.ai.application.dto.BreakdownResponse;
import com.kshitiz.taskforge.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST endpoints for AI features.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "AI")
public class AiController {

    private final TaskBreakdownService breakdownService;

    /**
     * Suggests subtasks for a task. Nothing is saved; send the ones to keep to
     * {@code POST /api/tasks/{id}/subtasks}.
     *
     * @param taskId task to split up
     * @param actor  the logged-in user
     * @return suggestions (429 when today's limit is reached, 503 if AI is unavailable)
     */
    @PostMapping("/api/tasks/{taskId}/ai/breakdown")
    @Operation(summary = "Suggest subtasks for a task with AI")
    public BreakdownResponse suggestBreakdown(@PathVariable UUID taskId, @AuthenticationPrincipal CurrentUser actor) {
        return breakdownService.suggestBreakdown(taskId, actor);
    }

    /**
     * Tells whether AI is available and how many requests the user has left today.
     *
     * @param actor the logged-in user
     * @return AI status
     */
    @GetMapping("/api/ai/status")
    @Operation(summary = "AI availability and remaining requests today")
    public AiStatusResponse status(@AuthenticationPrincipal CurrentUser actor) {
        return breakdownService.status(actor);
    }
}
