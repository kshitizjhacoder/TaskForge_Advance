package com.kshitiz.taskforge.task.application;

import com.kshitiz.taskforge.common.domain.PageQuery;
import com.kshitiz.taskforge.common.domain.PageResult;
import com.kshitiz.taskforge.common.exception.ForbiddenOperationException;
import com.kshitiz.taskforge.common.exception.InvalidRequestException;
import com.kshitiz.taskforge.common.exception.ResourceNotFoundException;
import com.kshitiz.taskforge.common.security.CurrentUser;
import com.kshitiz.taskforge.task.application.dto.ChangeTaskStatusCommand;
import com.kshitiz.taskforge.task.application.dto.CreateTaskCommand;
import com.kshitiz.taskforge.task.application.dto.TaskResponse;
import com.kshitiz.taskforge.task.application.dto.UpdateTaskCommand;
import com.kshitiz.taskforge.task.domain.model.Task;
import com.kshitiz.taskforge.task.domain.model.TaskSearchCriteria;
import com.kshitiz.taskforge.task.domain.policy.TaskAccessPolicy;
import com.kshitiz.taskforge.task.domain.port.TaskAssignmentRepository;
import com.kshitiz.taskforge.task.domain.port.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use cases for tasks: create, read, search, edit, change status and delete.
 *
 * <p>Business rules (valid dates, allowed status moves, tag limits) live in the {@link Task}
 * domain object and permission rules in {@link TaskAccessPolicy}; this service loads data,
 * applies both, and saves. Methods that change data receive the logged-in user
 * ({@code actor}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskAssignmentRepository assignmentRepository;

    /**
     * Creates a task in status TO_DO, owned by the logged-in user.
     *
     * @param command validated request data
     * @param actor   the logged-in user, recorded as the creator
     * @return the created task
     */
    @Transactional
    public TaskResponse createTask(CreateTaskCommand command, CurrentUser actor) {
        Task task = Task.create(command.projectId(), command.title(), command.description(), actor.id(),
                command.startDate(), command.endDate(), command.priority(), command.tags());
        Task saved = taskRepository.save(task);
        log.info("User {} created task {}", actor.id(), saved.getId());
        return TaskResponse.from(saved);
    }

    /**
     * Returns one task.
     *
     * @param id task ID
     * @return the task
     * @throws ResourceNotFoundException if no such task exists
     */
    public TaskResponse getTask(UUID id) {
        return TaskResponse.from(loadTask(id));
    }

    /**
     * Returns one page of tasks matching optional filters.
     *
     * @param criteria  filters (status, priority, text, dates, ...)
     * @param pageQuery page, size and sort
     * @return matching tasks
     * @throws InvalidRequestException if dueFrom is after dueTo
     */
    public PageResult<TaskResponse> searchTasks(TaskSearchCriteria criteria, PageQuery pageQuery) {
        if (criteria.dueFrom() != null && criteria.dueTo() != null && criteria.dueFrom().isAfter(criteria.dueTo())) {
            throw new InvalidRequestException("dueFrom must be on or before dueTo");
        }
        return taskRepository.search(criteria, pageQuery).map(TaskResponse::from);
    }

    /**
     * Replaces a task's editable details.
     *
     * @param id      task ID
     * @param command validated new details
     * @param actor   the logged-in user
     * @return the updated task
     * @throws ResourceNotFoundException   if no such task exists
     * @throws ForbiddenOperationException if the user may not manage this task
     */
    @Transactional
    public TaskResponse updateTask(UUID id, UpdateTaskCommand command, CurrentUser actor) {
        Task task = loadManageableTask(id, actor);
        task.updateDetails(command.title(), command.description(), command.startDate(), command.endDate(),
                command.priority(), command.tags());
        return TaskResponse.from(taskRepository.save(task));
    }

    /**
     * Moves a task to another status, following the workflow rules.
     *
     * @param id      task ID
     * @param command the requested status
     * @param actor   the logged-in user
     * @return the updated task
     * @throws ResourceNotFoundException   if no such task exists
     * @throws ForbiddenOperationException if the user is not creator, assignee, manager or admin
     */
    @Transactional
    public TaskResponse changeStatus(UUID id, ChangeTaskStatusCommand command, CurrentUser actor) {
        Task task = loadTask(id);
        TaskAccessPolicy.requireCanChangeStatus(task, actor, assignmentRepository.exists(id, actor.id()));
        task.changeStatus(command.status());
        Task saved = taskRepository.save(task);
        log.info("User {} moved task {} to {}", actor.id(), id, saved.getStatus());
        return TaskResponse.from(saved);
    }

    /**
     * Soft-deletes a task.
     *
     * @param id    task ID
     * @param actor the logged-in user
     * @throws ResourceNotFoundException   if no such task exists
     * @throws ForbiddenOperationException if the user may not manage this task
     */
    @Transactional
    public void deleteTask(UUID id, CurrentUser actor) {
        loadManageableTask(id, actor);
        taskRepository.deleteById(id);
        log.info("User {} deleted task {}", actor.id(), id);
    }

    /**
     * Checks that a task exists. Used before attaching comments to a task.
     *
     * @param id task ID
     * @throws ResourceNotFoundException if no such task exists
     */
    public void ensureTaskExists(UUID id) {
        if (!taskRepository.existsById(id)) {
            throw ResourceNotFoundException.of("Task", id);
        }
    }

    /**
     * Checks that a task exists and that the user may manage it (edit, delete, assign).
     * Shared with {@link TaskAssignmentService} so the rule is applied the same way.
     *
     * @param id    task ID
     * @param actor the logged-in user
     * @return the task
     * @throws ResourceNotFoundException   if no such task exists
     * @throws ForbiddenOperationException if the user may not manage it
     */
    Task loadManageableTask(UUID id, CurrentUser actor) {
        Task task = loadTask(id);
        TaskAccessPolicy.requireCanManage(task, actor);
        return task;
    }

    /**
     * Loads a task or throws 404.
     *
     * @param id task ID
     * @return the task
     */
    private Task loadTask(UUID id) {
        return taskRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Task", id));
    }
}
