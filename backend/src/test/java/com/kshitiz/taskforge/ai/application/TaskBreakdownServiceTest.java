package com.kshitiz.taskforge.ai.application;

import com.kshitiz.taskforge.ai.application.dto.BreakdownResponse;
import com.kshitiz.taskforge.ai.domain.model.BreakdownRequest;
import com.kshitiz.taskforge.ai.domain.model.SubtaskSuggestion;
import com.kshitiz.taskforge.ai.domain.port.AiAssistant;
import com.kshitiz.taskforge.common.exception.AiInvalidResponseException;
import com.kshitiz.taskforge.common.exception.AiUnavailableException;
import com.kshitiz.taskforge.common.exception.BusinessRuleViolationException;
import com.kshitiz.taskforge.common.security.CurrentUser;
import com.kshitiz.taskforge.project.application.ProjectService;
import com.kshitiz.taskforge.project.application.dto.ProjectResponse;
import com.kshitiz.taskforge.task.application.TaskService;
import com.kshitiz.taskforge.task.application.dto.TaskResponse;
import com.kshitiz.taskforge.task.domain.model.Priority;
import com.kshitiz.taskforge.task.domain.model.TaskStatus;
import com.kshitiz.taskforge.user.domain.model.Role;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TaskBreakdownService}: guardrails, retry and metrics.
 */
@ExtendWith(MockitoExtension.class)
class TaskBreakdownServiceTest {

    @Mock
    private TaskService taskService;
    @Mock
    private ProjectService projectService;
    @Mock
    private AiAssistant aiAssistant;
    @Mock
    private AiQuotaService quotaService;

    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
    private TaskBreakdownService service;
    private final CurrentUser actor = new CurrentUser(UUID.randomUUID(), "a@test.dev", Role.MEMBER);
    private final UUID projectId = UUID.randomUUID();
    private final UUID taskId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new TaskBreakdownService(taskService, projectService, aiAssistant, quotaService,
                new AiProperties(10, 100, Duration.ofSeconds(5), 8), meters);
    }

    /** Builds a task response; {@code parentTaskId} non-null makes it a subtask. */
    private TaskResponse task(UUID parentTaskId, String description) {
        return new TaskResponse(taskId, projectId, parentTaskId, "Build login page", description, actor.id(), null,
                null, Priority.HIGH, TaskStatus.TO_DO, List.of("frontend"), Instant.now(), Instant.now(), 0L);
    }

    /** Makes the task and its project visible to the actor. */
    private void taskIsVisible(TaskResponse task) {
        when(aiAssistant.isEnabled()).thenReturn(true);
        when(aiAssistant.providerName()).thenReturn("fake");
        when(taskService.getTask(taskId, actor)).thenReturn(task);
        when(projectService.getProject(projectId, actor))
                .thenReturn(new ProjectResponse(projectId, "TF", "TaskForge", null, actor.id(), null, null));
    }

    @Test
    void disabledAiFailsFastWithoutUsingQuota() {
        when(aiAssistant.isEnabled()).thenReturn(false);

        assertThatThrownBy(() -> service.suggestBreakdown(taskId, actor)).isInstanceOf(AiUnavailableException.class);
        verifyNoInteractions(quotaService, taskService);
    }

    @Test
    void subtasksCannotBeBrokenDown() {
        when(aiAssistant.isEnabled()).thenReturn(true);
        when(taskService.getTask(taskId, actor)).thenReturn(task(UUID.randomUUID(), null));

        assertThatThrownBy(() -> service.suggestBreakdown(taskId, actor))
                .isInstanceOf(BusinessRuleViolationException.class);
        verifyNoInteractions(quotaService);
    }

    @Test
    void returnsCleanSuggestionsAndSendsOnlyTaskText() {
        taskIsVisible(task(null, "x".repeat(10_000)));
        when(aiAssistant.suggestSubtasks(any())).thenReturn(List.of(
                new SubtaskSuggestion(" Design form ", null, Priority.HIGH),
                new SubtaskSuggestion("Build login page", null, Priority.LOW)));
        when(quotaService.remainingToday(actor.id())).thenReturn(9);

        BreakdownResponse response = service.suggestBreakdown(taskId, actor);

        assertThat(response.suggestions()).extracting(SubtaskSuggestion::title).containsExactly("Design form");
        assertThat(response.remainingToday()).isEqualTo(9);
        verify(quotaService).reserve(actor.id());
        ArgumentCaptor<BreakdownRequest> sent = ArgumentCaptor.forClass(BreakdownRequest.class);
        verify(aiAssistant).suggestSubtasks(sent.capture());
        assertThat(sent.getValue().projectName()).isEqualTo("TaskForge");
        assertThat(sent.getValue().taskDescription()).hasSize(TaskBreakdownService.MAX_DESCRIPTION_CHARS);
        assertThat(meters.counter("taskforge.ai.breakdown", "outcome", "success", "provider", "fake").count())
                .isEqualTo(1);
    }

    @Test
    void retriesOnceAfterUnreadableAnswer() {
        taskIsVisible(task(null, null));
        when(aiAssistant.suggestSubtasks(any()))
                .thenThrow(new AiInvalidResponseException("bad json"))
                .thenReturn(List.of(new SubtaskSuggestion("Design form", null, Priority.HIGH)));

        assertThat(service.suggestBreakdown(taskId, actor).suggestions()).hasSize(1);
        verify(aiAssistant, times(2)).suggestSubtasks(any());
    }

    @Test
    void failsWith502AfterTwoUselessAnswers() {
        taskIsVisible(task(null, null));
        when(aiAssistant.suggestSubtasks(any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.suggestBreakdown(taskId, actor))
                .isInstanceOf(AiInvalidResponseException.class);
        verify(aiAssistant, times(2)).suggestSubtasks(any());
        assertThat(meters.counter("taskforge.ai.breakdown", "outcome", "invalid", "provider", "fake").count())
                .isEqualTo(1);
    }
}
