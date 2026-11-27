package com.kshitiz.taskforge.task.application;

import com.kshitiz.taskforge.common.domain.PageQuery;
import com.kshitiz.taskforge.common.domain.PageResult;
import com.kshitiz.taskforge.common.exception.BusinessRuleViolationException;
import com.kshitiz.taskforge.common.exception.ForbiddenOperationException;
import com.kshitiz.taskforge.common.exception.InvalidRequestException;
import com.kshitiz.taskforge.common.exception.ResourceNotFoundException;
import com.kshitiz.taskforge.common.security.CurrentUser;
import com.kshitiz.taskforge.project.application.ProjectService;
import com.kshitiz.taskforge.project.domain.model.Project;
import com.kshitiz.taskforge.task.application.dto.ChangeTaskStatusCommand;
import com.kshitiz.taskforge.task.application.dto.CreateSubtasksCommand;
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

import java.util.List;
import java.util.UUID;

/**
 * Use cases for tasks: create, read, search, edit, change status and delete.
 *
 * <p>Every method first checks the user may see the task's project (member or admin, via
 * {@link ProjectService}); task-level permissions then come from {@link TaskAccessPolicy}
 * and business rules from the {@link Task} domain object.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskAssignmentRepository assignmentRepository;
    private final ProjectService projectService;

    /**
     * Creates a task in a project, owned by the logged-in user. If a parent task is given,
     * the new task is created as its subtask.
     *
     * @param command validated request data (including the project and optional parent)
     * @param actor   the logged-in user, recorded as the creator
     * @return the created task
     * @throws ResourceNotFoundException      if the project or parent does not exist or is not visible
     * @throws BusinessRuleViolationException if the parent is in another project or is itself a subtask
     */
    @Transactional
    public TaskResponse createTask(CreateTaskCommand command, CurrentUser actor) {
        projectService.requireAccessibleProject(command.projectId(), actor);
        Task task;
        if (command.parentTaskId() == null) {
            task = Task.create(command.projectId(), command.title(), command.description(), actor.id(),
                    command.startDate(), command.endDate(), command.priority(), command.tags());
        } else {
            Task parent = loadAccessibleTask(command.parentTaskId(), actor).task();
            if (!parent.getProjectId().equals(command.projectId())) {
                throw new BusinessRuleViolationException("A subtask must be in the same project as its parent");
            }
            task = Task.createSubtask(parent, command.title(), command.description(), actor.id(),
                    command.startDate(), command.endDate(), command.priority(), command.tags());
        }
        Task saved = taskRepository.save(task);
        log.info("User {} created task {} in project {}", actor.id(), saved.getId(), saved.getProjectId());
        return TaskResponse.from(saved);
    }

    /**
     * Creates several subtasks of a task in one go, for example the AI suggestions the user
     * chose to keep. All are created or none (one transaction).
     *
     * @param parentTaskId the task being split up
     * @param command      the subtasks to create
     * @param actor        the logged-in user, recorded as the creator
     * @return the created subtasks, in the order given
     * @throws ResourceNotFoundException      if the parent is not found or not visible
     * @throws BusinessRuleViolationException if the parent is itself a subtask
     */
    @Transactional
    public List<TaskResponse> createSubtasks(UUID parentTaskId, CreateSubtasksCommand command, CurrentUser actor) {
        Task parent = loadAccessibleTask(parentTaskId, actor).task();
        List<TaskResponse> created = command.subtasks().stream()
                .map(item -> Task.createSubtask(parent, item.title(), item.description(), actor.id(),
                        null, null, item.priority(), null))
                .map(taskRepository::save)
                .map(TaskResponse::from)
                .toList();
        log.info("User {} created {} subtasks of task {}", actor.id(), created.size(), parentTaskId);
        return created;
    }

    /**
     * Returns one task.
     *
     * @param id    task ID
     * @param actor the logged-in user
     * @return the task
     * @throws ResourceNotFoundException if the task does not exist or its project is not visible
     */
    public TaskResponse getTask(UUID id, CurrentUser actor) {
        return TaskResponse.from(loadAccessibleTask(id, actor).task());
    }

    /**
     * Returns one page of tasks matching optional filters, limited to the projects the user
     * belongs to (admins see all projects).
     *
     * @param criteria  client filters (status, priority, text, dates, ...)
     * @param pageQuery page, size and sort
     * @param actor     the logged-in user
     * @return matching tasks
     * @throws InvalidRequestException   if dueFrom is after dueTo
     * @throws ResourceNotFoundException if filtering by a project the user cannot see
     */
    public PageResult<TaskResponse> searchTasks(TaskSearchCriteria criteria, PageQuery pageQuery, CurrentUser actor) {
        if (criteria.dueFrom() != null && criteria.dueTo() != null && criteria.dueFrom().isAfter(criteria.dueTo())) {
            throw new InvalidRequestException("dueFrom must be on or before dueTo");
        }
        if (criteria.projectId() != null) {
            projectService.requireAccessibleProject(criteria.projectId(), actor);
        }
        TaskSearchCriteria limited = criteria.limitedTo(projectService.visibleProjectIds(actor).orElse(null));
        return taskRepository.search(limited, pageQuery).map(TaskResponse::from);
    }

    /**
     * Replaces a task's editable details.
     *
     * @param id      task ID
     * @param command validated new details
     * @param actor   the logged-in user
     * @return the updated task
     * @throws ResourceNotFoundException   if the task is not found or not visible
     * @throws ForbiddenOperationException if the user may not manage this task
     */
    @Transactional
    public TaskResponse updateTask(UUID id, UpdateTaskCommand command, CurrentUser actor) {
        Task task = loadManageableTask(id, actor).task();
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
     * @throws ResourceNotFoundException   if the task is not found or not visible
     * @throws ForbiddenOperationException if the user is not allowed to change the status
     */
    @Transactional
    public TaskResponse changeStatus(UUID id, ChangeTaskStatusCommand command, CurrentUser actor) {
        AccessibleTask accessible = loadAccessibleTask(id, actor);
        Task task = accessible.task();
        TaskAccessPolicy.requireCanChangeStatus(task, actor, assignmentRepository.exists(id, actor.id()),
                accessible.isProjectOwner(actor));
        task.changeStatus(command.status());
        Task saved = taskRepository.save(task);
        log.info("User {} moved task {} to {}", actor.id(), id, saved.getStatus());
        return TaskResponse.from(saved);
    }

    /**
     * Soft-deletes a task together with its subtasks.
     *
     * @param id    task ID
     * @param actor the logged-in user
     * @throws ResourceNotFoundException   if the task is not found or not visible
     * @throws ForbiddenOperationException if the user may not manage this task
     */
    @Transactional
    public void deleteTask(UUID id, CurrentUser actor) {
        loadManageableTask(id, actor);
        taskRepository.findSubtaskIds(id).forEach(taskRepository::deleteById);
        taskRepository.deleteById(id);
        log.info("User {} deleted task {}", actor.id(), id);
    }

    /**
     * Loads a task and checks the user may see its project. Shared with the comment,
     * assignment and attachment services so every one of them applies the same check.
     *
     * @param id    task ID
     * @param actor the logged-in user
     * @return the task with its project
     * @throws ResourceNotFoundException if the task does not exist or its project is not visible
     */
    AccessibleTask loadAccessibleTask(UUID id, CurrentUser actor) {
        Task task = taskRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Task", id));
        Project project = projectService.requireAccessibleProject(task.getProjectId(), actor);
        return new AccessibleTask(task, project);
    }

    /**
     * Loads a task and checks the user may manage it (edit, delete, assign).
     *
     * @param id    task ID
     * @param actor the logged-in user
     * @return the task with its project
     * @throws ResourceNotFoundException   if the task is not found or not visible
     * @throws ForbiddenOperationException if the user may not manage it
     */
    AccessibleTask loadManageableTask(UUID id, CurrentUser actor) {
        AccessibleTask accessible = loadAccessibleTask(id, actor);
        TaskAccessPolicy.requireCanManage(accessible.task(), actor, accessible.isProjectOwner(actor));
        return accessible;
    }
}
