package com.kshitiz.taskforge.task.application;

import com.kshitiz.taskforge.common.domain.PageQuery;
import com.kshitiz.taskforge.common.domain.PageResult;
import com.kshitiz.taskforge.common.exception.InvalidRequestException;
import com.kshitiz.taskforge.common.exception.ResourceNotFoundException;
import com.kshitiz.taskforge.task.application.dto.ChangeTaskStatusCommand;
import com.kshitiz.taskforge.task.application.dto.CreateTaskCommand;
import com.kshitiz.taskforge.task.application.dto.TaskResponse;
import com.kshitiz.taskforge.task.application.dto.UpdateTaskCommand;
import com.kshitiz.taskforge.task.domain.model.Task;
import com.kshitiz.taskforge.task.domain.model.TaskSearchCriteria;
import com.kshitiz.taskforge.task.domain.port.TaskRepository;
import com.kshitiz.taskforge.user.application.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use cases for tasks: create, read, search, edit, change status and delete.
 *
 * <p>The rules themselves (valid dates, allowed status moves, tag limits) live in the
 * {@link Task} domain object; this service loads, calls the domain, and saves.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserService userService;

    /**
     * Creates a task in status TO_DO.
     *
     * @param command validated request data
     * @return the created task
     * @throws ResourceNotFoundException if the creator does not exist
     */
    @Transactional
    public TaskResponse createTask(CreateTaskCommand command) {
        userService.ensureUserExists(command.createdBy());
        Task task = Task.create(command.projectId(), command.title(), command.description(), command.createdBy(),
                command.startDate(), command.endDate(), command.priority(), command.tags());
        Task saved = taskRepository.save(task);
        log.info("Created task {}", saved.getId());
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
     * @return the updated task
     * @throws ResourceNotFoundException if no such task exists
     */
    @Transactional
    public TaskResponse updateTask(UUID id, UpdateTaskCommand command) {
        Task task = loadTask(id);
        task.updateDetails(command.title(), command.description(), command.startDate(), command.endDate(),
                command.priority(), command.tags());
        return TaskResponse.from(taskRepository.save(task));
    }

    /**
     * Moves a task to another status, following the workflow rules.
     *
     * @param id      task ID
     * @param command the requested status
     * @return the updated task
     * @throws ResourceNotFoundException if no such task exists
     */
    @Transactional
    public TaskResponse changeStatus(UUID id, ChangeTaskStatusCommand command) {
        Task task = loadTask(id);
        task.changeStatus(command.status());
        Task saved = taskRepository.save(task);
        log.info("Task {} moved to {}", id, saved.getStatus());
        return TaskResponse.from(saved);
    }

    /**
     * Soft-deletes a task.
     *
     * @param id task ID
     * @throws ResourceNotFoundException if no such task exists
     */
    @Transactional
    public void deleteTask(UUID id) {
        ensureTaskExists(id);
        taskRepository.deleteById(id);
        log.info("Deleted task {}", id);
    }

    /**
     * Checks that a task exists. Used by the comment and assignment services before they
     * attach anything to a task.
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
     * Loads a task or throws 404.
     *
     * @param id task ID
     * @return the task
     */
    private Task loadTask(UUID id) {
        return taskRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Task", id));
    }
}
