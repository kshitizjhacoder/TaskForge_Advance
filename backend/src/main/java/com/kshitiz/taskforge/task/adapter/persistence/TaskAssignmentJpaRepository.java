package com.kshitiz.taskforge.task.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
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
     * Lists a user's assignments, skipping tasks that have been deleted (the join to
     * {@code TaskJpaEntity} applies its soft-delete filter).
     *
     * @param userId user ID
     * @return assignments on existing tasks, newest first
     */
    @Query("""
            select a from TaskAssignmentJpaEntity a
            where a.userId = :userId
              and exists (select t.id from TaskJpaEntity t where t.id = a.taskId)
            order by a.assignedAt desc
            """)
    List<TaskAssignmentJpaEntity> findActiveByUserId(@Param("userId") UUID userId);
}
