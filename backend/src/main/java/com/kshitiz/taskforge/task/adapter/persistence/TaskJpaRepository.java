package com.kshitiz.taskforge.task.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data repository for {@link TaskJpaEntity}. {@link JpaSpecificationExecutor} adds
 * the dynamic filtering used by task search.
 */
public interface TaskJpaRepository extends JpaRepository<TaskJpaEntity, UUID>,
        JpaSpecificationExecutor<TaskJpaEntity> {

    /**
     * Lists the IDs of a task's non-deleted subtasks.
     *
     * @param parentTaskId parent task ID
     * @return subtask IDs
     */
    @Query("select t.id from TaskJpaEntity t where t.parentTaskId = :parentTaskId")
    List<UUID> findSubtaskIds(@Param("parentTaskId") UUID parentTaskId);
}
