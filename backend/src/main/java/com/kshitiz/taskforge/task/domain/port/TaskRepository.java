package com.kshitiz.taskforge.task.domain.port;

import com.kshitiz.taskforge.common.domain.PageQuery;
import com.kshitiz.taskforge.common.domain.PageResult;
import com.kshitiz.taskforge.task.domain.model.Task;
import com.kshitiz.taskforge.task.domain.model.TaskSearchCriteria;

import java.util.Optional;
import java.util.UUID;

/**
 * Port (interface) for storing and loading tasks. Implemented by
 * {@code TaskPersistenceAdapter}. Deleted tasks are invisible to every method.
 */
public interface TaskRepository {

    /**
     * Inserts a new task or updates an existing one.
     *
     * @param task task to store
     * @return the stored task with timestamps and version filled in
     */
    Task save(Task task);

    /**
     * Finds a task by ID.
     *
     * @param id task ID
     * @return the task, or empty if not found
     */
    Optional<Task> findById(UUID id);

    /**
     * Checks whether a task exists.
     *
     * @param id task ID
     * @return {@code true} if it exists and is not deleted
     */
    boolean existsById(UUID id);

    /**
     * Finds one page of tasks matching the filters.
     *
     * @param criteria  optional filters
     * @param pageQuery page, size and sort
     * @return matching tasks
     */
    PageResult<Task> search(TaskSearchCriteria criteria, PageQuery pageQuery);

    /**
     * Soft-deletes a task (the row is kept but hidden).
     *
     * @param id task ID
     */
    void deleteById(UUID id);
}
