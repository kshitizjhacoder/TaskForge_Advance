package com.kshitiz.taskforge.project.domain.model;

import com.kshitiz.taskforge.common.exception.BusinessRuleViolationException;
import com.kshitiz.taskforge.common.exception.ForbiddenOperationException;
import com.kshitiz.taskforge.common.exception.ResourceNotFoundException;
import com.kshitiz.taskforge.common.security.CurrentUser;
import com.kshitiz.taskforge.project.domain.policy.ProjectAccessPolicy;
import com.kshitiz.taskforge.user.domain.model.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link Project} rules and {@link ProjectAccessPolicy}.
 */
class ProjectTest {

    private final CurrentUser owner = new CurrentUser(UUID.randomUUID(), "owner@test.dev", Role.MANAGER);
    private final CurrentUser member = new CurrentUser(UUID.randomUUID(), "member@test.dev", Role.MEMBER);
    private final CurrentUser admin = new CurrentUser(UUID.randomUUID(), "admin@test.dev", Role.ADMIN);
    private final Project project = Project.create("tf", " TaskForge ", null, owner.id());

    @Test
    void keyIsUpperCasedAndNameTrimmed() {
        assertThat(project.getKey()).isEqualTo("TF");
        assertThat(project.getName()).isEqualTo("TaskForge");
        assertThat(project.isOwnedBy(owner.id())).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"T", "1TF", "TOOLONGKEY1", "T-F", ""})
    void rejectsBadKeys(String key) {
        assertThatThrownBy(() -> Project.create(key, "Name", null, owner.id()))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void nonMembersGet404NotForbidden() {
        CurrentUser stranger = new CurrentUser(UUID.randomUUID(), "x@test.dev", Role.MANAGER);

        assertThatThrownBy(() -> ProjectAccessPolicy.requireCanView(project, stranger, false))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatCode(() -> ProjectAccessPolicy.requireCanView(project, member, true)).doesNotThrowAnyException();
        assertThatCode(() -> ProjectAccessPolicy.requireCanView(project, admin, false))
                .as("admins see every project").doesNotThrowAnyException();
    }

    @Test
    void onlyOwnerOrAdminManages() {
        assertThatCode(() -> ProjectAccessPolicy.requireCanManage(project, owner)).doesNotThrowAnyException();
        assertThatCode(() -> ProjectAccessPolicy.requireCanManage(project, admin)).doesNotThrowAnyException();
        assertThatThrownBy(() -> ProjectAccessPolicy.requireCanManage(project, member))
                .isInstanceOf(ForbiddenOperationException.class);
    }
}
