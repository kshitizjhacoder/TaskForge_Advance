package com.kshitiz.taskforge.task.adapter.rest;

import com.kshitiz.taskforge.common.security.CurrentUser;
import com.kshitiz.taskforge.task.application.TaskAssignmentService;
import com.kshitiz.taskforge.task.application.dto.AssignUserCommand;
import com.kshitiz.taskforge.task.application.dto.AssignmentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for assigning users to tasks.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Task assignments")
public class TaskAssignmentController {

    private final TaskAssignmentService assignmentService;

    /**
     * Assigns a user to a task.
     *
     * @param taskId  task ID
     * @param command the user to assign
     * @param actor   the logged-in user
     * @return 201 Created with the assignment (409 if already assigned, 403 if not allowed)
     */
    @PostMapping("/api/tasks/{taskId}/assignees")
    @Operation(summary = "Assign a user to a task")
    public ResponseEntity<AssignmentResponse> assignUser(@PathVariable UUID taskId,
                                                         @Valid @RequestBody AssignUserCommand command,
                                                         @AuthenticationPrincipal CurrentUser actor) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assignmentService.assignUser(taskId, command, actor));
    }

    /**
     * Lists who is assigned to a task.
     *
     * @param taskId task ID
     * @param actor  the logged-in user
     * @return assignments
     */
    @GetMapping("/api/tasks/{taskId}/assignees")
    @Operation(summary = "List a task's assignees")
    public List<AssignmentResponse> listAssignees(@PathVariable UUID taskId,
                                                  @AuthenticationPrincipal CurrentUser actor) {
        return assignmentService.listAssignees(taskId, actor);
    }

    /**
     * Removes a user from a task.
     *
     * @param taskId task ID
     * @param userId user ID
     * @param actor  the logged-in user
     * @return 204 No Content (403 if not allowed)
     */
    @DeleteMapping("/api/tasks/{taskId}/assignees/{userId}")
    @Operation(summary = "Remove a user from a task")
    public ResponseEntity<Void> unassignUser(@PathVariable UUID taskId, @PathVariable UUID userId,
                                             @AuthenticationPrincipal CurrentUser actor) {
        assignmentService.unassignUser(taskId, userId, actor);
        return ResponseEntity.noContent().build();
    }
}
