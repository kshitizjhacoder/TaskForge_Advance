package com.kshitiz.taskforge.task.domain.port;

import com.kshitiz.taskforge.task.domain.model.TaskAssignment;

import java.util.List;
import java.util.UUID;

/**
 * Port (interface) for task assignments. Implemented by
 * {@code TaskAssignmentPersistenceAdapter}. Removing an assignment deletes it for good;
 * there is nothing worth keeping.
 */
public interface TaskAssignmentRepository {

    /**
     * Stores a new assignment.
     *
     * @param assignment assignment to store
     * @return the stored assignment
     */
    TaskAssignment save(TaskAssignment assignment);

    /**
     * Checks whether a user is already assigned to a task.
     *
     * @param taskId task ID
     * @param userId user ID
     * @return {@code true} if the assignment exists
     */
    boolean exists(UUID taskId, UUID userId);

    /**
     * Removes a user from a task.
     *
     * @param taskId task ID
     * @param userId user ID
     */
    void delete(UUID taskId, UUID userId);

    /**
     * Lists who is assigned to a task, earliest assignment first.
     *
     * @param taskId task ID
     * @return assignments
     */
    List<TaskAssignment> findByTaskId(UUID taskId);

    /**
     * Removes a user from every task of a project. Used when the user leaves the project.
     *
     * @param projectId project ID
     * @param userId    user ID
     * @return number of assignments removed
     */
    int deleteByProjectAndUser(UUID projectId, UUID userId);
}
