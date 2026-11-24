package com.kshitiz.taskforge.task.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data repository for {@link TaskCommentJpaEntity}.
 */
public interface TaskCommentJpaRepository extends JpaRepository<TaskCommentJpaEntity, UUID> {

    /**
     * Lists a task's comments in the order they were written.
     *
     * @param taskId task ID
     * @return comments, oldest first
     */
    List<TaskCommentJpaEntity> findByTaskIdOrderByCreatedAtAsc(UUID taskId);
}
