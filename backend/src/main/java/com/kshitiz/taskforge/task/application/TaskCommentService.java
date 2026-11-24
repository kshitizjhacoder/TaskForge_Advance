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
 * Use cases for task comments: add, list, edit and delete. Only the author may edit a
 * comment; the author or an admin may delete it (see {@link TaskAccessPolicy}).
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
     * @throws ResourceNotFoundException if the task does not exist
     */
    @Transactional
    public CommentResponse addComment(UUID taskId, AddCommentCommand command, CurrentUser actor) {
        taskService.ensureTaskExists(taskId);
        TaskComment comment = TaskComment.write(taskId, actor.id(), command.content());
        return CommentResponse.from(commentRepository.save(comment));
    }

    /**
     * Lists the comments on a task, oldest first.
     *
     * @param taskId task ID
     * @return comments
     * @throws ResourceNotFoundException if the task does not exist
     */
    public List<CommentResponse> listComments(UUID taskId) {
        taskService.ensureTaskExists(taskId);
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
     * @throws ResourceNotFoundException   if the comment does not exist
     * @throws ForbiddenOperationException if the user is not the author
     */
    @Transactional
    public CommentResponse editComment(UUID commentId, EditCommentCommand command, CurrentUser actor) {
        TaskComment comment = loadComment(commentId);
        TaskAccessPolicy.requireCanEditComment(comment, actor);
        comment.edit(command.content());
        return CommentResponse.from(commentRepository.save(comment));
    }

    /**
     * Soft-deletes a comment.
     *
     * @param commentId comment ID
     * @param actor     the logged-in user
     * @throws ResourceNotFoundException   if the comment does not exist
     * @throws ForbiddenOperationException if the user is neither the author nor an admin
     */
    @Transactional
    public void deleteComment(UUID commentId, CurrentUser actor) {
        TaskAccessPolicy.requireCanDeleteComment(loadComment(commentId), actor);
        commentRepository.deleteById(commentId);
    }

    /**
     * Loads a comment or throws 404.
     *
     * @param id comment ID
     * @return the comment
     */
    private TaskComment loadComment(UUID id) {
        return commentRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Comment", id));
    }
}
