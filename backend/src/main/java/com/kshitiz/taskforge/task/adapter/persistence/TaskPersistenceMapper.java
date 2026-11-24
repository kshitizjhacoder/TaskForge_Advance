package com.kshitiz.taskforge.task.adapter.persistence;

import com.kshitiz.taskforge.task.domain.model.Task;
import com.kshitiz.taskforge.task.domain.model.TaskAssignment;
import com.kshitiz.taskforge.task.domain.model.TaskComment;
import org.mapstruct.Mapper;

/**
 * Converts the task feature's domain objects to and from their JPA entities. MapStruct
 * generates the implementation at compile time; fields are matched by name.
 */
@Mapper
public interface TaskPersistenceMapper {

    /**
     * Rebuilds a domain task from a database row.
     *
     * @param entity database row
     * @return domain task
     */
    Task toDomain(TaskJpaEntity entity);

    /**
     * Copies a domain task into a database row ready to be saved.
     *
     * @param task domain task
     * @return database row
     */
    TaskJpaEntity toEntity(Task task);

    /**
     * Rebuilds a domain comment from a database row.
     *
     * @param entity database row
     * @return domain comment
     */
    TaskComment toDomain(TaskCommentJpaEntity entity);

    /**
     * Copies a domain comment into a database row.
     *
     * @param comment domain comment
     * @return database row
     */
    TaskCommentJpaEntity toEntity(TaskComment comment);

    /**
     * Rebuilds a domain assignment from a database row.
     *
     * @param entity database row
     * @return domain assignment
     */
    TaskAssignment toDomain(TaskAssignmentJpaEntity entity);

    /**
     * Copies a domain assignment into a database row.
     *
     * @param assignment domain assignment
     * @return database row
     */
    TaskAssignmentJpaEntity toEntity(TaskAssignment assignment);
}
