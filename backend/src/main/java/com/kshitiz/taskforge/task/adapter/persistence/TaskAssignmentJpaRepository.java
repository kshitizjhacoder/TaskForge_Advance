package com.kshitiz.taskforge.task.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data repository for {@link TaskAssignmentJpaEntity}.
 */
public interface TaskAssignmentJpaRepository
        extends JpaRepository<TaskAssignmentJpaEntity, TaskAssignmentJpaEntity.Key> {

    /**
     * Lists who is assigned to a task.
     *
     * @param taskId task ID
     * @return assignments, earliest first
     */
    List<TaskAssignmentJpaEntity> findByTaskIdOrderByAssignedAtAsc(UUID taskId);

    /**
     * Deletes a user's assignments on all tasks of a project, in one statement.
     *
     * @param projectId project ID
     * @param userId    user ID
     * @return number of rows deleted
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from TaskAssignmentJpaEntity a
            where a.userId = :userId
              and a.taskId in (select t.id from TaskJpaEntity t where t.projectId = :projectId)
            """)
    int deleteByProjectAndUser(@Param("projectId") UUID projectId, @Param("userId") UUID userId);
}
