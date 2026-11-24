package com.kshitiz.taskforge.task.application;

import com.kshitiz.taskforge.common.exception.ResourceNotFoundException;
import com.kshitiz.taskforge.task.application.dto.AddCommentCommand;
import com.kshitiz.taskforge.task.application.dto.CommentResponse;
import com.kshitiz.taskforge.task.application.dto.EditCommentCommand;
import com.kshitiz.taskforge.task.domain.model.TaskComment;
import com.kshitiz.taskforge.task.domain.port.TaskCommentRepository;
import com.kshitiz.taskforge.user.application.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Use cases for task comments: add, list, edit and delete.
 *
 * <p>From Phase 2, only the author (or an admin) may edit or delete a comment.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskCommentService {

    private final TaskCommentRepository commentRepository;
    private final TaskService taskService;
    private final UserService userService;

    /**
     * Adds a comment to a task.
     *
     * @param taskId  task ID
     * @param command author and text
     * @return the created comment
     * @throws ResourceNotFoundException if the task or the author does not exist
     */
    @Transactional
    public CommentResponse addComment(UUID taskId, AddCommentCommand command) {
        taskService.ensureTaskExists(taskId);
        userService.ensureUserExists(command.authorId());
        TaskComment comment = TaskComment.write(taskId, command.authorId(), command.content());
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
     * @return the updated comment
     * @throws ResourceNotFoundException if the comment does not exist
     */
    @Transactional
    public CommentResponse editComment(UUID commentId, EditCommentCommand command) {
        TaskComment comment = loadComment(commentId);
        comment.edit(command.content());
        return CommentResponse.from(commentRepository.save(comment));
    }

    /**
     * Soft-deletes a comment.
     *
     * @param commentId comment ID
     * @throws ResourceNotFoundException if the comment does not exist
     */
    @Transactional
    public void deleteComment(UUID commentId) {
        loadComment(commentId);
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
