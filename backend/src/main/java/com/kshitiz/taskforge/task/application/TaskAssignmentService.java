package com.kshitiz.taskforge.task.application;

import com.kshitiz.taskforge.common.exception.DuplicateResourceException;
import com.kshitiz.taskforge.common.exception.ForbiddenOperationException;
import com.kshitiz.taskforge.common.exception.ResourceNotFoundException;
import com.kshitiz.taskforge.common.security.CurrentUser;
import com.kshitiz.taskforge.task.application.dto.AssignUserCommand;
import com.kshitiz.taskforge.task.application.dto.AssignmentResponse;
import com.kshitiz.taskforge.task.domain.model.TaskAssignment;
import com.kshitiz.taskforge.task.domain.port.TaskAssignmentRepository;
import com.kshitiz.taskforge.user.application.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Use cases for assigning users to tasks. Assigning and unassigning follow the same rule as
 * editing a task: creator, manager or admin.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskAssignmentService {

    private final TaskAssignmentRepository assignmentRepository;
    private final TaskService taskService;
    private final UserService userService;

    /**
     * Assigns a user to a task.
     *
     * @param taskId  task ID
     * @param command the user to assign
     * @param actor   the logged-in user
     * @return the new assignment
     * @throws ResourceNotFoundException   if the task or user does not exist
     * @throws ForbiddenOperationException if the actor may not manage the task
     * @throws DuplicateResourceException  if the user is already assigned
     */
    @Transactional
    public AssignmentResponse assignUser(UUID taskId, AssignUserCommand command, CurrentUser actor) {
        taskService.loadManageableTask(taskId, actor);
        userService.ensureUserExists(command.userId());
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
     * @throws ResourceNotFoundException   if the task does not exist or the user is not assigned
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
     * @return assignments
     * @throws ResourceNotFoundException if the task does not exist
     */
    public List<AssignmentResponse> listAssignees(UUID taskId) {
        taskService.ensureTaskExists(taskId);
        return assignmentRepository.findByTaskId(taskId).stream().map(AssignmentResponse::from).toList();
    }

    /**
     * Lists the tasks a user is assigned to.
     *
     * @param userId user ID
     * @return assignments
     * @throws ResourceNotFoundException if the user does not exist
     */
    public List<AssignmentResponse> listAssignmentsOfUser(UUID userId) {
        userService.ensureUserExists(userId);
        return assignmentRepository.findByUserId(userId).stream().map(AssignmentResponse::from).toList();
    }
}
