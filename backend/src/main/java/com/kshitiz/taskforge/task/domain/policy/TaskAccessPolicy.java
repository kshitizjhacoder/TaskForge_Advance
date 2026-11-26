package com.kshitiz.taskforge.task.domain.policy;

import com.kshitiz.taskforge.common.exception.ForbiddenOperationException;
import com.kshitiz.taskforge.common.security.CurrentUser;
import com.kshitiz.taskforge.task.domain.model.Task;
import com.kshitiz.taskforge.task.domain.model.TaskAttachment;
import com.kshitiz.taskforge.task.domain.model.TaskComment;

/**
 * Who may change what in the task feature. Every rule lives here, so it can be read in one
 * place and unit-tested without Spring.
 *
 * <p>All of these apply only <em>after</em> the project check: the user must already be a
 * member of the task's project (or an admin), see {@code ProjectAccessPolicy}.
 *
 * <pre>
 *   Action                         Allowed for
 *   Edit / delete / (un)assign     task creator, project owner, MANAGER, ADMIN
 *   Change status                  the above, plus an assignee
 *   Edit a comment                 comment author only
 *   Delete a comment               comment author, ADMIN
 *   Delete an attachment           uploader, plus everyone who may edit the task
 * </pre>
 * Reading tasks and adding tasks, comments or attachments is open to every project member.
 */
public final class TaskAccessPolicy {

    private TaskAccessPolicy() {
        // static rules only, not meant to be instantiated
    }

    /**
     * Checks that the user may edit, delete or change the assignees of a task.
     *
     * @param task           the task
     * @param actor          the logged-in user
     * @param isProjectOwner whether the user owns the task's project
     * @throws ForbiddenOperationException if not allowed
     */
    public static void requireCanManage(Task task, CurrentUser actor, boolean isProjectOwner) {
        if (!canManage(task, actor, isProjectOwner)) {
            throw new ForbiddenOperationException(
                    "Only the task's creator, the project owner, a manager or an admin can do this");
        }
    }

    /**
     * Checks that the user may change a task's status.
     *
     * @param task           the task
     * @param actor          the logged-in user
     * @param isAssignee     whether the user is assigned to the task
     * @param isProjectOwner whether the user owns the task's project
     * @throws ForbiddenOperationException if not allowed
     */
    public static void requireCanChangeStatus(Task task, CurrentUser actor, boolean isAssignee,
                                              boolean isProjectOwner) {
        if (!isAssignee && !canManage(task, actor, isProjectOwner)) {
            throw new ForbiddenOperationException(
                    "Only the task's creator, an assignee, the project owner, a manager or an admin can change its status");
        }
    }

    /**
     * Checks that the user may edit a comment.
     *
     * @param comment the comment
     * @param actor   the logged-in user
     * @throws ForbiddenOperationException if the user did not write the comment
     */
    public static void requireCanEditComment(TaskComment comment, CurrentUser actor) {
        if (!actor.is(comment.getAuthorId())) {
            throw new ForbiddenOperationException("Only the author can edit a comment");
        }
    }

    /**
     * Checks that the user may delete a comment.
     *
     * @param comment the comment
     * @param actor   the logged-in user
     * @throws ForbiddenOperationException if the user is neither the author nor an admin
     */
    public static void requireCanDeleteComment(TaskComment comment, CurrentUser actor) {
        if (!actor.is(comment.getAuthorId()) && !actor.isAdmin()) {
            throw new ForbiddenOperationException("Only the author or an admin can delete a comment");
        }
    }

    /**
     * Checks that the user may delete an attachment.
     *
     * @param attachment     the attachment
     * @param task           the task it belongs to
     * @param actor          the logged-in user
     * @param isProjectOwner whether the user owns the task's project
     * @throws ForbiddenOperationException if the user is not the uploader and may not manage the task
     */
    public static void requireCanDeleteAttachment(TaskAttachment attachment, Task task, CurrentUser actor,
                                                  boolean isProjectOwner) {
        if (!actor.is(attachment.getUploadedBy()) && !canManage(task, actor, isProjectOwner)) {
            throw new ForbiddenOperationException(
                    "Only the uploader, the task's creator, the project owner, a manager or an admin can delete this file");
        }
    }

    /**
     * The shared "may manage this task" rule used by several checks above.
     *
     * @param task           the task
     * @param actor          the logged-in user
     * @param isProjectOwner whether the user owns the task's project
     * @return {@code true} for the creator, the project owner, managers and admins
     */
    private static boolean canManage(Task task, CurrentUser actor, boolean isProjectOwner) {
        return actor.is(task.getCreatedBy()) || isProjectOwner || actor.isManagerOrAdmin();
    }
}
