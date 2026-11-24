package com.kshitiz.taskforge.task.domain.policy;

import com.kshitiz.taskforge.common.exception.ForbiddenOperationException;
import com.kshitiz.taskforge.common.security.CurrentUser;
import com.kshitiz.taskforge.task.domain.model.Task;
import com.kshitiz.taskforge.task.domain.model.TaskComment;

/**
 * Who may change what in the task feature. Every permission rule lives here, so it can be
 * read in one place and unit-tested without Spring.
 *
 * <pre>
 *   Action                         Allowed for
 *   Edit / delete / (un)assign     task creator, MANAGER, ADMIN
 *   Change status                  task creator, an assignee, MANAGER, ADMIN
 *   Edit a comment                 comment author only
 *   Delete a comment               comment author, ADMIN
 * </pre>
 * Reading and creating tasks or comments is open to every logged-in user (Phase 3 adds
 * project-membership checks).
 */
public final class TaskAccessPolicy {

    private TaskAccessPolicy() {
        // static rules only, not meant to be instantiated
    }

    /**
     * Checks that the user may edit, delete or change the assignees of a task.
     *
     * @param task  the task
     * @param actor the logged-in user
     * @throws ForbiddenOperationException if not allowed
     */
    public static void requireCanManage(Task task, CurrentUser actor) {
        if (!actor.is(task.getCreatedBy()) && !actor.isManagerOrAdmin()) {
            throw new ForbiddenOperationException("Only the task's creator, a manager or an admin can do this");
        }
    }

    /**
     * Checks that the user may change a task's status.
     *
     * @param task       the task
     * @param actor      the logged-in user
     * @param isAssignee whether the user is assigned to the task
     * @throws ForbiddenOperationException if not allowed
     */
    public static void requireCanChangeStatus(Task task, CurrentUser actor, boolean isAssignee) {
        if (!isAssignee && !actor.is(task.getCreatedBy()) && !actor.isManagerOrAdmin()) {
            throw new ForbiddenOperationException(
                    "Only the task's creator, an assignee, a manager or an admin can change its status");
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
}
