package com.kshitiz.taskforge.project.application;

import com.kshitiz.taskforge.common.exception.BusinessRuleViolationException;
import com.kshitiz.taskforge.common.exception.DuplicateResourceException;
import com.kshitiz.taskforge.common.exception.ResourceNotFoundException;
import com.kshitiz.taskforge.common.security.CurrentUser;
import com.kshitiz.taskforge.project.application.dto.CreateProjectCommand;
import com.kshitiz.taskforge.project.domain.event.ProjectMemberRemovedEvent;
import com.kshitiz.taskforge.project.domain.model.Project;
import com.kshitiz.taskforge.project.domain.model.ProjectMember;
import com.kshitiz.taskforge.project.domain.port.ProjectMemberRepository;
import com.kshitiz.taskforge.project.domain.port.ProjectRepository;
import com.kshitiz.taskforge.user.application.UserService;
import com.kshitiz.taskforge.user.domain.model.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ProjectService} with mocked repositories.
 */
@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectMemberRepository memberRepository;
    @Mock
    private UserService userService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @InjectMocks
    private ProjectService projectService;

    private final CurrentUser owner = new CurrentUser(UUID.randomUUID(), "owner@test.dev", Role.MANAGER);
    private final CurrentUser member = new CurrentUser(UUID.randomUUID(), "member@test.dev", Role.MEMBER);

    @Test
    void createAddsOwnerAsFirstMember() {
        when(projectRepository.existsByKey("TF")).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenAnswer(call -> call.getArgument(0));

        projectService.createProject(new CreateProjectCommand("tf", "TaskForge", null), owner);

        ArgumentCaptor<ProjectMember> membership = ArgumentCaptor.forClass(ProjectMember.class);
        verify(memberRepository).save(membership.capture());
        assertThat(membership.getValue().userId()).isEqualTo(owner.id());
    }

    @Test
    void duplicateKeyIsRejected() {
        when(projectRepository.existsByKey("TF")).thenReturn(true);

        assertThatThrownBy(() -> projectService.createProject(new CreateProjectCommand("TF", "Again", null), owner))
                .isInstanceOf(DuplicateResourceException.class);
        verify(projectRepository, never()).save(any());
    }

    @Test
    void nonMemberCannotSeeProject() {
        Project project = Project.create("TF", "TaskForge", null, owner.id());
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(memberRepository.exists(project.getId(), member.id())).thenReturn(false);

        assertThatThrownBy(() -> projectService.getProject(project.getId(), member))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void ownerCannotBeRemoved() {
        Project project = Project.create("TF", "TaskForge", null, owner.id());
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(memberRepository.exists(project.getId(), owner.id())).thenReturn(true);

        assertThatThrownBy(() -> projectService.removeMember(project.getId(), owner.id(), owner))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void removingMemberPublishesEvent() {
        Project project = Project.create("TF", "TaskForge", null, owner.id());
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(memberRepository.exists(project.getId(), owner.id())).thenReturn(true);
        when(memberRepository.exists(project.getId(), member.id())).thenReturn(true);

        projectService.removeMember(project.getId(), member.id(), owner);

        verify(memberRepository).delete(project.getId(), member.id());
        verify(eventPublisher).publishEvent(new ProjectMemberRemovedEvent(project.getId(), member.id()));
    }

    @Test
    void adminsSearchAllProjectsOthersOnlyTheirs() {
        CurrentUser admin = new CurrentUser(UUID.randomUUID(), "admin@test.dev", Role.ADMIN);
        List<UUID> memberProjects = List.of(UUID.randomUUID());
        when(memberRepository.findProjectIdsByUserId(member.id())).thenReturn(memberProjects);

        assertThat(projectService.visibleProjectIds(admin)).isEmpty();
        assertThat(projectService.visibleProjectIds(member)).contains(memberProjects);
    }
}
