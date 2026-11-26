package com.kshitiz.taskforge.task.adapter.rest;

import com.kshitiz.taskforge.common.adapter.rest.RestResponses;
import com.kshitiz.taskforge.common.security.CurrentUser;
import com.kshitiz.taskforge.task.application.TaskCommentService;
import com.kshitiz.taskforge.task.application.dto.AddCommentCommand;
import com.kshitiz.taskforge.task.application.dto.CommentResponse;
import com.kshitiz.taskforge.task.application.dto.EditCommentCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for task comments. Comments are created and listed under their task
 * ({@code /api/tasks/{taskId}/comments}) and edited or deleted by their own ID
 * ({@code /api/comments/{id}}).
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Task comments")
public class TaskCommentController {

    private final TaskCommentService commentService;

    /**
     * Adds a comment to a task.
     *
     * @param taskId  task ID
     * @param command comment text
     * @param actor   the logged-in user (becomes the author)
     * @return 201 Created with the new comment
     */
    @PostMapping("/api/tasks/{taskId}/comments")
    @Operation(summary = "Comment on a task")
    public ResponseEntity<CommentResponse> addComment(@PathVariable UUID taskId,
                                                      @Valid @RequestBody AddCommentCommand command,
                                                      @AuthenticationPrincipal CurrentUser actor) {
        CommentResponse created = commentService.addComment(taskId, command, actor);
        return RestResponses.created(created, "/api/comments/{id}", created.id());
    }

    /**
     * Lists a task's comments, oldest first.
     *
     * @param taskId task ID
     * @param actor  the logged-in user
     * @return comments
     */
    @GetMapping("/api/tasks/{taskId}/comments")
    @Operation(summary = "List a task's comments")
    public List<CommentResponse> listComments(@PathVariable UUID taskId, @AuthenticationPrincipal CurrentUser actor) {
        return commentService.listComments(taskId, actor);
    }

    /**
     * Edits a comment's text.
     *
     * @param id      comment ID
     * @param command new text
     * @param actor   the logged-in user
     * @return the updated comment (403 if not the author)
     */
    @PutMapping("/api/comments/{id}")
    @Operation(summary = "Edit a comment")
    public CommentResponse editComment(@PathVariable UUID id, @Valid @RequestBody EditCommentCommand command,
                                       @AuthenticationPrincipal CurrentUser actor) {
        return commentService.editComment(id, command, actor);
    }

    /**
     * Soft-deletes a comment.
     *
     * @param id    comment ID
     * @param actor the logged-in user
     * @return 204 No Content (403 if neither author nor admin)
     */
    @DeleteMapping("/api/comments/{id}")
    @Operation(summary = "Delete a comment")
    public ResponseEntity<Void> deleteComment(@PathVariable UUID id, @AuthenticationPrincipal CurrentUser actor) {
        commentService.deleteComment(id, actor);
        return ResponseEntity.noContent().build();
    }
}
