package com.kshitiz.taskforge.task.adapter.rest;

import com.kshitiz.taskforge.common.adapter.rest.PageParams;
import com.kshitiz.taskforge.common.adapter.rest.RestResponses;
import com.kshitiz.taskforge.common.domain.PageResult;
import com.kshitiz.taskforge.common.security.CurrentUser;
import com.kshitiz.taskforge.task.application.TaskService;
import com.kshitiz.taskforge.task.application.dto.ChangeTaskStatusCommand;
import com.kshitiz.taskforge.task.application.dto.CreateTaskCommand;
import com.kshitiz.taskforge.task.application.dto.TaskResponse;
import com.kshitiz.taskforge.task.application.dto.UpdateTaskCommand;
import com.kshitiz.taskforge.task.domain.model.Priority;
import com.kshitiz.taskforge.task.domain.model.TaskSearchCriteria;
import com.kshitiz.taskforge.task.domain.model.TaskStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * REST endpoints for tasks. All need a logged-in user; the logged-in user ({@code actor}) is
 * injected from the access token by {@code @AuthenticationPrincipal}.
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks")
public class TaskController {

    private final TaskService taskService;

    /**
     * Creates a task owned by the logged-in user.
     *
     * @param command validated request body
     * @param actor   the logged-in user (becomes the creator)
     * @return 201 Created with the new task and its URL in the Location header
     */
    @PostMapping
    @Operation(summary = "Create a task")
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody CreateTaskCommand command,
                                                   @AuthenticationPrincipal CurrentUser actor) {
        TaskResponse created = taskService.createTask(command, actor);
        return RestResponses.created(created, "/api/tasks/{id}", created.id());
    }

    /**
     * Returns one task.
     *
     * @param id    task ID
     * @param actor the logged-in user
     * @return the task (404 if not found or not in one of the user's projects)
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get a task by ID")
    public TaskResponse getTask(@PathVariable UUID id, @AuthenticationPrincipal CurrentUser actor) {
        return taskService.getTask(id, actor);
    }

    /**
     * Searches tasks in the user's projects (all projects for admins). Every filter is
     * optional and they combine with AND.
     *
     * @param q          text contained in the title
     * @param status     exact status
     * @param priority   exact priority
     * @param projectId  project ID
     * @param createdBy  creator's user ID
     * @param assigneeId a user assigned to the task
     * @param dueFrom    due on or after this date (yyyy-MM-dd)
     * @param dueTo      due on or before this date (yyyy-MM-dd)
     * @param pageParams page, size, sort (createdAt, updatedAt, title, endDate) and direction
     * @param actor      the logged-in user
     * @return one page of tasks
     */
    @GetMapping
    @Operation(summary = "Search tasks with filters and paging")
    public PageResult<TaskResponse> searchTasks(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) UUID createdBy,
            @RequestParam(required = false) UUID assigneeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueTo,
            @ParameterObject PageParams pageParams,
            @AuthenticationPrincipal CurrentUser actor) {
        TaskSearchCriteria criteria = TaskSearchCriteria.of(q, status, priority, projectId, createdBy, assigneeId,
                dueFrom, dueTo);
        return taskService.searchTasks(criteria, pageParams.toPageQuery("createdAt"), actor);
    }

    /**
     * Replaces a task's editable details.
     *
     * @param id      task ID
     * @param command validated new details
     * @param actor   the logged-in user
     * @return the updated task (403 if not creator, manager or admin)
     */
    @PutMapping("/{id}")
    @Operation(summary = "Edit a task's details")
    public TaskResponse updateTask(@PathVariable UUID id, @Valid @RequestBody UpdateTaskCommand command,
                                   @AuthenticationPrincipal CurrentUser actor) {
        return taskService.updateTask(id, command, actor);
    }

    /**
     * Moves a task to another status (e.g. TO_DO → IN_PROGRESS).
     *
     * @param id      task ID
     * @param command the new status
     * @param actor   the logged-in user
     * @return the updated task (422 if the move is not allowed, 403 if the user may not)
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "Change a task's status")
    public TaskResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody ChangeTaskStatusCommand command,
                                     @AuthenticationPrincipal CurrentUser actor) {
        return taskService.changeStatus(id, command, actor);
    }

    /**
     * Soft-deletes a task.
     *
     * @param id    task ID
     * @param actor the logged-in user
     * @return 204 No Content (403 if not creator, manager or admin)
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a task")
    public ResponseEntity<Void> deleteTask(@PathVariable UUID id, @AuthenticationPrincipal CurrentUser actor) {
        taskService.deleteTask(id, actor);
        return ResponseEntity.noContent().build();
    }
}
