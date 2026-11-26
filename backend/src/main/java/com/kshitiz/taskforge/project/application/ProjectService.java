package com.kshitiz.taskforge.project.application;

import com.kshitiz.taskforge.common.domain.PageQuery;
import com.kshitiz.taskforge.common.domain.PageResult;
import com.kshitiz.taskforge.common.exception.BusinessRuleViolationException;
import com.kshitiz.taskforge.common.exception.DuplicateResourceException;
import com.kshitiz.taskforge.common.exception.ForbiddenOperationException;
import com.kshitiz.taskforge.common.exception.ResourceNotFoundException;
import com.kshitiz.taskforge.common.security.CurrentUser;
import com.kshitiz.taskforge.project.application.dto.AddMemberCommand;
import com.kshitiz.taskforge.project.application.dto.CreateProjectCommand;
import com.kshitiz.taskforge.project.application.dto.ProjectMemberResponse;
import com.kshitiz.taskforge.project.application.dto.ProjectResponse;
import com.kshitiz.taskforge.project.application.dto.UpdateProjectCommand;
import com.kshitiz.taskforge.project.domain.event.ProjectMemberRemovedEvent;
import com.kshitiz.taskforge.project.domain.model.Project;
import com.kshitiz.taskforge.project.domain.model.ProjectMember;
import com.kshitiz.taskforge.project.domain.policy.ProjectAccessPolicy;
import com.kshitiz.taskforge.project.domain.port.ProjectMemberRepository;
import com.kshitiz.taskforge.project.domain.port.ProjectRepository;
import com.kshitiz.taskforge.user.application.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Use cases for projects and their members, plus the access checks other features use
 * ({@link #requireAccessibleProject}, {@link #visibleProjectIds}).
 *
 * <p>Permission rules live in {@link ProjectAccessPolicy}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Creates a project owned by the logged-in user, who also becomes its first member.
     * (Only managers and admins may call this; enforced on the controller.)
     *
     * @param command key, name and description
     * @param actor   the logged-in user, who becomes the owner
     * @return the created project
     * @throws DuplicateResourceException     if the key is already used
     * @throws BusinessRuleViolationException if the key or name is invalid
     */
    @Transactional
    public ProjectResponse createProject(CreateProjectCommand command, CurrentUser actor) {
        Project project = Project.create(command.key(), command.name(), command.description(), actor.id());
        if (projectRepository.existsByKey(project.getKey())) {
            throw new DuplicateResourceException("Project key " + project.getKey() + " is already in use");
        }
        Project saved = projectRepository.save(project);
        memberRepository.save(ProjectMember.of(saved.getId(), actor.id()));
        log.info("User {} created project {} ({})", actor.id(), saved.getId(), saved.getKey());
        return ProjectResponse.from(saved);
    }

    /**
     * Returns one project.
     *
     * @param id    project ID
     * @param actor the logged-in user
     * @return the project
     * @throws ResourceNotFoundException if it does not exist or the user is not a member
     */
    public ProjectResponse getProject(UUID id, CurrentUser actor) {
        return ProjectResponse.from(requireAccessibleProject(id, actor));
    }

    /**
     * Lists the projects the user can see: all of them for admins, otherwise the ones the
     * user is a member of.
     *
     * @param actor     the logged-in user
     * @param pageQuery page, size and sort
     * @return one page of projects
     */
    public PageResult<ProjectResponse> listProjects(CurrentUser actor, PageQuery pageQuery) {
        PageResult<Project> page = actor.isAdmin()
                ? projectRepository.findAll(pageQuery)
                : projectRepository.findByMember(actor.id(), pageQuery);
        return page.map(ProjectResponse::from);
    }

    /**
     * Changes a project's name and description.
     *
     * @param id      project ID
     * @param command new details
     * @param actor   the logged-in user
     * @return the updated project
     * @throws ResourceNotFoundException   if not found or not visible to the user
     * @throws ForbiddenOperationException if the user is neither owner nor admin
     */
    @Transactional
    public ProjectResponse updateProject(UUID id, UpdateProjectCommand command, CurrentUser actor) {
        Project project = loadManageableProject(id, actor);
        project.updateDetails(command.name(), command.description());
        return ProjectResponse.from(projectRepository.save(project));
    }

    /**
     * Soft-deletes a project. Its tasks become unreachable because every task request
     * checks the project first.
     *
     * @param id    project ID
     * @param actor the logged-in user
     * @throws ResourceNotFoundException   if not found or not visible to the user
     * @throws ForbiddenOperationException if the user is neither owner nor admin
     */
    @Transactional
    public void deleteProject(UUID id, CurrentUser actor) {
        loadManageableProject(id, actor);
        projectRepository.deleteById(id);
        log.info("User {} deleted project {}", actor.id(), id);
    }

    /**
     * Lists a project's members.
     *
     * @param id    project ID
     * @param actor the logged-in user
     * @return members, the owner flagged
     * @throws ResourceNotFoundException if not found or not visible to the user
     */
    public List<ProjectMemberResponse> listMembers(UUID id, CurrentUser actor) {
        Project project = requireAccessibleProject(id, actor);
        return memberRepository.findByProjectId(id).stream()
                .map(member -> ProjectMemberResponse.from(member, project.getOwnerId()))
                .toList();
    }

    /**
     * Adds a user to a project.
     *
     * @param id      project ID
     * @param command the user to add
     * @param actor   the logged-in user
     * @return the new membership
     * @throws ResourceNotFoundException   if the project or user does not exist
     * @throws ForbiddenOperationException if the actor is neither owner nor admin
     * @throws DuplicateResourceException  if the user is already a member
     */
    @Transactional
    public ProjectMemberResponse addMember(UUID id, AddMemberCommand command, CurrentUser actor) {
        Project project = loadManageableProject(id, actor);
        userService.ensureUserExists(command.userId());
        if (memberRepository.exists(id, command.userId())) {
            throw new DuplicateResourceException("This user is already a member of the project");
        }
        ProjectMember saved = memberRepository.save(ProjectMember.of(id, command.userId()));
        return ProjectMemberResponse.from(saved, project.getOwnerId());
    }

    /**
     * Removes a user from a project. Their assignments on the project's tasks are removed
     * too, by publishing a {@link ProjectMemberRemovedEvent} that the task feature handles in
     * the same transaction.
     *
     * @param id     project ID
     * @param userId user to remove
     * @param actor  the logged-in user
     * @throws ResourceNotFoundException      if the project does not exist or the user is not a member
     * @throws ForbiddenOperationException    if the actor is neither owner nor admin
     * @throws BusinessRuleViolationException if trying to remove the owner
     */
    @Transactional
    public void removeMember(UUID id, UUID userId, CurrentUser actor) {
        Project project = loadManageableProject(id, actor);
        if (project.isOwnedBy(userId)) {
            throw new BusinessRuleViolationException("The project owner cannot be removed from the project");
        }
        if (!memberRepository.exists(id, userId)) {
            throw new ResourceNotFoundException("User " + userId + " is not a member of project " + id);
        }
        memberRepository.delete(id, userId);
        eventPublisher.publishEvent(new ProjectMemberRemovedEvent(id, userId));
        log.info("User {} removed {} from project {}", actor.id(), userId, id);
    }

    /**
     * Loads a project the user may see (a member, or an admin). Other features call this
     * before reading or changing anything inside a project.
     *
     * @param id    project ID
     * @param actor the logged-in user
     * @return the project
     * @throws ResourceNotFoundException if it does not exist or the user may not see it
     */
    public Project requireAccessibleProject(UUID id, CurrentUser actor) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Project", id));
        ProjectAccessPolicy.requireCanView(project, actor, actor.isAdmin() || memberRepository.exists(id, actor.id()));
        return project;
    }

    /**
     * Checks whether a user belongs to a project. Used, for example, before assigning a
     * user to one of the project's tasks.
     *
     * @param projectId project ID
     * @param userId    user ID
     * @return {@code true} if the user is a member
     */
    public boolean isMember(UUID projectId, UUID userId) {
        return memberRepository.exists(projectId, userId);
    }

    /**
     * Returns the projects whose tasks a user may search.
     *
     * @param actor the logged-in user
     * @return empty for admins (meaning "no restriction"), otherwise the IDs of the user's projects
     */
    public Optional<List<UUID>> visibleProjectIds(CurrentUser actor) {
        return actor.isAdmin() ? Optional.empty() : Optional.of(memberRepository.findProjectIdsByUserId(actor.id()));
    }

    /**
     * Loads a project and checks the user may manage it (owner or admin).
     *
     * @param id    project ID
     * @param actor the logged-in user
     * @return the project
     */
    private Project loadManageableProject(UUID id, CurrentUser actor) {
        Project project = requireAccessibleProject(id, actor);
        ProjectAccessPolicy.requireCanManage(project, actor);
        return project;
    }
}
