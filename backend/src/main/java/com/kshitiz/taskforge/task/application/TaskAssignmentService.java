package com.kshitiz.taskforge.task.application;

import com.kshitiz.taskforge.common.exception.BusinessRuleViolationException;
import com.kshitiz.taskforge.common.exception.DuplicateResourceException;
import com.kshitiz.taskforge.common.exception.ForbiddenOperationException;
import com.kshitiz.taskforge.common.exception.ResourceNotFoundException;
import com.kshitiz.taskforge.common.security.CurrentUser;
import com.kshitiz.taskforge.project.application.ProjectService;
import com.kshitiz.taskforge.project.domain.event.ProjectMemberRemovedEvent;
import com.kshitiz.taskforge.task.application.dto.AssignUserCommand;
import com.kshitiz.taskforge.task.application.dto.AssignmentResponse;
import com.kshitiz.taskforge.task.domain.model.TaskAssignment;
import com.kshitiz.taskforge.task.domain.port.TaskAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Use cases for assigning users to tasks. Assigning and unassigning follow the same rule as
 * editing a task, and only members of the task's project can be assigned.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskAssignmentService {

    private final TaskAssignmentRepository assignmentRepository;
    private final TaskService taskService;
    private final ProjectService projectService;

    /**
     * Assigns a user to a task.
     *
     * @param taskId  task ID
     * @param command the user to assign
     * @param actor   the logged-in user
     * @return the new assignment
     * @throws ResourceNotFoundException      if the task is not found or not visible
     * @throws ForbiddenOperationException    if the actor may not manage the task
     * @throws BusinessRuleViolationException if the user is not a member of the task's project
     * @throws DuplicateResourceException     if the user is already assigned
     */
    @Transactional
    public AssignmentResponse assignUser(UUID taskId, AssignUserCommand command, CurrentUser actor) {
        AccessibleTask accessible = taskService.loadManageableTask(taskId, actor);
        if (!projectService.isMember(accessible.project().getId(), command.userId())) {
            throw new BusinessRuleViolationException("Only members of the task's project can be assigned to it");
        }
        if (assignmentRepository.exists(taskId, command.userId())) {
            throw new DuplicateResourceException("This user is already assigned to the task");
        }
        return AssignmentResponse.from(assignmentRepository.save(TaskAssignment.of(taskId, command.userId())));
    }

    /**
     * Removes a user from a task.
     *
     * @param taskId task ID
     * @param userId user ID
     * @param actor  the logged-in user
     * @throws ResourceNotFoundException   if the task is not visible or the user is not assigned
     * @throws ForbiddenOperationException if the actor may not manage the task
     */
    @Transactional
    public void unassignUser(UUID taskId, UUID userId, CurrentUser actor) {
        taskService.loadManageableTask(taskId, actor);
        if (!assignmentRepository.exists(taskId, userId)) {
            throw new ResourceNotFoundException("User " + userId + " is not assigned to task " + taskId);
        }
        assignmentRepository.delete(taskId, userId);
    }

    /**
     * Lists who is assigned to a task.
     *
     * @param taskId task ID
     * @param actor  the logged-in user
     * @return assignments
     * @throws ResourceNotFoundException if the task is not found or not visible
     */
    public List<AssignmentResponse> listAssignees(UUID taskId, CurrentUser actor) {
        taskService.loadAccessibleTask(taskId, actor);
        return assignmentRepository.findByTaskId(taskId).stream().map(AssignmentResponse::from).toList();
    }

    /**
     * When a user leaves a project, removes them from all of that project's tasks. Runs
     * inside the transaction that removed the membership, so both happen or neither does.
     *
     * @param event which user left which project
     */
    @EventListener
    @Transactional
    public void onProjectMemberRemoved(ProjectMemberRemovedEvent event) {
        int removed = assignmentRepository.deleteByProjectAndUser(event.projectId(), event.userId());
        log.info("Removed {} task assignments of user {} after leaving project {}", removed, event.userId(),
                event.projectId());
    }
}
