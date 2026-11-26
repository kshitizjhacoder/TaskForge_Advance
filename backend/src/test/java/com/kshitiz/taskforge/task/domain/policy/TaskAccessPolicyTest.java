package com.kshitiz.taskforge.task.domain.policy;

import com.kshitiz.taskforge.common.exception.ForbiddenOperationException;
import com.kshitiz.taskforge.common.security.CurrentUser;
import com.kshitiz.taskforge.task.domain.model.Task;
import com.kshitiz.taskforge.task.domain.model.TaskAttachment;
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

    private static final boolean OWNER = true;
    private static final boolean NOT_OWNER = false;

    private final CurrentUser creator = user(Role.MEMBER);
    private final CurrentUser otherMember = user(Role.MEMBER);
    private final CurrentUser manager = user(Role.MANAGER);
    private final CurrentUser admin = user(Role.ADMIN);
    private final Task task = Task.create(UUID.randomUUID(), "Task", null, creator.id(), null, null, null, null);
    private final TaskComment comment = TaskComment.write(task.getId(), creator.id(), "Hello");
    private final TaskAttachment attachment =
            TaskAttachment.upload(task.getId(), "notes.txt", "text/plain", 10, otherMember.id());

    /** Creates a logged-in user with a random ID. */
    private static CurrentUser user(Role role) {
        return new CurrentUser(UUID.randomUUID(), role + "@test.dev", role);
    }

    @Test
    void manageTask() {
        assertThatCode(() -> TaskAccessPolicy.requireCanManage(task, creator, NOT_OWNER)).doesNotThrowAnyException();
        assertThatCode(() -> TaskAccessPolicy.requireCanManage(task, manager, NOT_OWNER)).doesNotThrowAnyException();
        assertThatCode(() -> TaskAccessPolicy.requireCanManage(task, admin, NOT_OWNER)).doesNotThrowAnyException();
        assertThatCode(() -> TaskAccessPolicy.requireCanManage(task, otherMember, OWNER))
                .as("the project owner manages every task in the project").doesNotThrowAnyException();
        assertThatThrownBy(() -> TaskAccessPolicy.requireCanManage(task, otherMember, NOT_OWNER))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void changeStatus() {
        assertThatCode(() -> TaskAccessPolicy.requireCanChangeStatus(task, otherMember, true, NOT_OWNER))
                .as("an assignee may change status").doesNotThrowAnyException();
        assertThatCode(() -> TaskAccessPolicy.requireCanChangeStatus(task, creator, false, NOT_OWNER))
                .doesNotThrowAnyException();
        assertThatCode(() -> TaskAccessPolicy.requireCanChangeStatus(task, otherMember, false, OWNER))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> TaskAccessPolicy.requireCanChangeStatus(task, otherMember, false, NOT_OWNER))
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

    @Test
    void deleteAttachmentIsUploaderOrTaskManager() {
        CurrentUser uploader = new CurrentUser(attachment.getUploadedBy(), "up@test.dev", Role.MEMBER);
        CurrentUser stranger = user(Role.MEMBER);

        assertThatCode(() -> TaskAccessPolicy.requireCanDeleteAttachment(attachment, task, uploader, NOT_OWNER))
                .doesNotThrowAnyException();
        assertThatCode(() -> TaskAccessPolicy.requireCanDeleteAttachment(attachment, task, creator, NOT_OWNER))
                .as("the task creator may clean up files").doesNotThrowAnyException();
        assertThatThrownBy(() -> TaskAccessPolicy.requireCanDeleteAttachment(attachment, task, stranger, NOT_OWNER))
                .isInstanceOf(ForbiddenOperationException.class);
    }
}
