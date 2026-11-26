package com.kshitiz.taskforge.project.domain.policy;

import com.kshitiz.taskforge.common.exception.ForbiddenOperationException;
import com.kshitiz.taskforge.common.exception.ResourceNotFoundException;
import com.kshitiz.taskforge.common.security.CurrentUser;
import com.kshitiz.taskforge.project.domain.model.Project;

/**
 * Who may see and change a project.
 *
 * <pre>
 *   Action                                   Allowed for
 *   See the project and work on its tasks    members (incl. owner), ADMIN
 *   Edit, delete, add/remove members         owner, ADMIN
 *   Create a project                         MANAGER, ADMIN (checked on the controller)
 * </pre>
 * Non-members get "404 not found" rather than 403, so they cannot even learn that a
 * project exists.
 */
public final class ProjectAccessPolicy {

    private ProjectAccessPolicy() {
        // static rules only, not meant to be instantiated
    }

    /**
     * Checks that the user may see the project.
     *
     * @param project  the project
     * @param actor    the logged-in user
     * @param isMember whether the user is a member
     * @throws ResourceNotFoundException if the user is neither a member nor an admin
     */
    public static void requireCanView(Project project, CurrentUser actor, boolean isMember) {
        if (!isMember && !actor.isAdmin()) {
            throw ResourceNotFoundException.of("Project", project.getId());
        }
    }

    /**
     * Checks that the user may edit or delete the project and manage its members.
     *
     * @param project the project
     * @param actor   the logged-in user
     * @throws ForbiddenOperationException if the user is neither the owner nor an admin
     */
    public static void requireCanManage(Project project, CurrentUser actor) {
        if (!project.isOwnedBy(actor.id()) && !actor.isAdmin()) {
            throw new ForbiddenOperationException("Only the project owner or an admin can do this");
        }
    }
}
