package com.kshitiz.taskforge.task.application;

import com.kshitiz.taskforge.common.security.CurrentUser;
import com.kshitiz.taskforge.project.domain.model.Project;
import com.kshitiz.taskforge.task.domain.model.Task;

/**
 * A task the logged-in user is allowed to see, together with its project. Returned by
 * {@link TaskService#loadAccessibleTask} so the comment, assignment and attachment services
 * all apply the same project check and can ask whether the user owns the project.
 *
 * @param task    the task
 * @param project the project it belongs to
 */
public record AccessibleTask(Task task, Project project) {

    /**
     * Checks whether the user owns the task's project (which gives manager-like powers over
     * the project's tasks).
     *
     * @param actor the logged-in user
     * @return {@code true} for the project owner
     */
    public boolean isProjectOwner(CurrentUser actor) {
        return project.isOwnedBy(actor.id());
    }
}
