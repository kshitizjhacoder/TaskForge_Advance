package com.kshitiz.taskforge.task.domain.policy;

import com.kshitiz.taskforge.common.exception.ForbiddenOperationException;
import com.kshitiz.taskforge.common.security.CurrentUser;
import com.kshitiz.taskforge.task.domain.model.Task;
import com.kshitiz.taskforge.task.domain.model.TaskComment;
import com.kshitiz.taskforge.user.domain.model.Role;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for every row of the permission table in {@link TaskAccessPolicy}.
 */
class TaskAccessPolicyTest {

    private final CurrentUser creator = user(Role.MEMBER);
    private final CurrentUser otherMember = user(Role.MEMBER);
    private final CurrentUser manager = user(Role.MANAGER);
    private final CurrentUser admin = user(Role.ADMIN);
    private final Task task = Task.create(null, "Task", null, creator.id(), null, null, null, null);
    private final TaskComment comment = TaskComment.write(task.getId(), creator.id(), "Hello");

    /** Creates a logged-in user with a random ID. */
    private static CurrentUser user(Role role) {
        return new CurrentUser(UUID.randomUUID(), role + "@test.dev", role);
    }

    @Test
    void manageTask() {
        assertThatCode(() -> TaskAccessPolicy.requireCanManage(task, creator)).doesNotThrowAnyException();
        assertThatCode(() -> TaskAccessPolicy.requireCanManage(task, manager)).doesNotThrowAnyException();
        assertThatCode(() -> TaskAccessPolicy.requireCanManage(task, admin)).doesNotThrowAnyException();
        assertThatThrownBy(() -> TaskAccessPolicy.requireCanManage(task, otherMember))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void changeStatus() {
        assertThatCode(() -> TaskAccessPolicy.requireCanChangeStatus(task, otherMember, true))
                .as("an assignee may change status").doesNotThrowAnyException();
        assertThatCode(() -> TaskAccessPolicy.requireCanChangeStatus(task, creator, false)).doesNotThrowAnyException();
        assertThatCode(() -> TaskAccessPolicy.requireCanChangeStatus(task, manager, false)).doesNotThrowAnyException();
        assertThatThrownBy(() -> TaskAccessPolicy.requireCanChangeStatus(task, otherMember, false))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void editCommentIsAuthorOnly() {
        assertThatCode(() -> TaskAccessPolicy.requireCanEditComment(comment, creator)).doesNotThrowAnyException();
        assertThatThrownBy(() -> TaskAccessPolicy.requireCanEditComment(comment, admin))
                .as("not even an admin may rewrite someone else's words")
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void deleteCommentIsAuthorOrAdmin() {
        assertThatCode(() -> TaskAccessPolicy.requireCanDeleteComment(comment, creator)).doesNotThrowAnyException();
        assertThatCode(() -> TaskAccessPolicy.requireCanDeleteComment(comment, admin)).doesNotThrowAnyException();
        assertThatThrownBy(() -> TaskAccessPolicy.requireCanDeleteComment(comment, manager))
                .isInstanceOf(ForbiddenOperationException.class);
    }
}
