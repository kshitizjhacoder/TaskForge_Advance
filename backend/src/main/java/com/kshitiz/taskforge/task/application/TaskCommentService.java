package com.kshitiz.taskforge.task.application;

import com.kshitiz.taskforge.common.exception.ForbiddenOperationException;
import com.kshitiz.taskforge.common.exception.ResourceNotFoundException;
import com.kshitiz.taskforge.common.security.CurrentUser;
import com.kshitiz.taskforge.task.application.dto.AddCommentCommand;
import com.kshitiz.taskforge.task.application.dto.CommentResponse;
import com.kshitiz.taskforge.task.application.dto.EditCommentCommand;
import com.kshitiz.taskforge.task.domain.model.TaskComment;
import com.kshitiz.taskforge.task.domain.policy.TaskAccessPolicy;
import com.kshitiz.taskforge.task.domain.port.TaskCommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Use cases for task comments: add, list, edit and delete. The user must be able to see the
 * task's project; only the author may edit a comment, and the author or an admin may delete
 * it (see {@link TaskAccessPolicy}).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskCommentService {

    private final TaskCommentRepository commentRepository;
    private final TaskService taskService;

    /**
     * Adds a comment to a task, written by the logged-in user.
     *
     * @param taskId  task ID
     * @param command comment text
     * @param actor   the logged-in user, recorded as the author
     * @return the created comment
     * @throws ResourceNotFoundException if the task is not found or not visible
     */
    @Transactional
    public CommentResponse addComment(UUID taskId, AddCommentCommand command, CurrentUser actor) {
        taskService.loadAccessibleTask(taskId, actor);
        TaskComment comment = TaskComment.write(taskId, actor.id(), command.content());
        return CommentResponse.from(commentRepository.save(comment));
    }

    /**
     * Lists the comments on a task, oldest first.
     *
     * @param taskId task ID
     * @param actor  the logged-in user
     * @return comments
     * @throws ResourceNotFoundException if the task is not found or not visible
     */
    public List<CommentResponse> listComments(UUID taskId, CurrentUser actor) {
        taskService.loadAccessibleTask(taskId, actor);
        return commentRepository.findByTaskId(taskId).stream()
                .map(CommentResponse::from)
                .toList();
    }

    /**
     * Replaces the text of a comment.
     *
     * @param commentId comment ID
     * @param command   new text
     * @param actor     the logged-in user
     * @return the updated comment
     * @throws ResourceNotFoundException   if the comment is not found or not visible
     * @throws ForbiddenOperationException if the user is not the author
     */
    @Transactional
    public CommentResponse editComment(UUID commentId, EditCommentCommand command, CurrentUser actor) {
        TaskComment comment = loadAccessibleComment(commentId, actor);
        TaskAccessPolicy.requireCanEditComment(comment, actor);
        comment.edit(command.content());
        return CommentResponse.from(commentRepository.save(comment));
    }

    /**
     * Soft-deletes a comment.
     *
     * @param commentId comment ID
     * @param actor     the logged-in user
     * @throws ResourceNotFoundException   if the comment is not found or not visible
     * @throws ForbiddenOperationException if the user is neither the author nor an admin
     */
    @Transactional
    public void deleteComment(UUID commentId, CurrentUser actor) {
        TaskAccessPolicy.requireCanDeleteComment(loadAccessibleComment(commentId, actor), actor);
        commentRepository.deleteById(commentId);
    }

    /**
     * Loads a comment and checks the user may see its task's project.
     *
     * @param id    comment ID
     * @param actor the logged-in user
     * @return the comment
     */
    private TaskComment loadAccessibleComment(UUID id, CurrentUser actor) {
        TaskComment comment = commentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Comment", id));
        taskService.loadAccessibleTask(comment.getTaskId(), actor);
        return comment;
    }
}
