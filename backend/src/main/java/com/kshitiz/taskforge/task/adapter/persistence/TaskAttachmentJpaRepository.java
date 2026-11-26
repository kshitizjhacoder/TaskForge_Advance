package com.kshitiz.taskforge.task.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data repository for {@link TaskAttachmentJpaEntity}.
 */
public interface TaskAttachmentJpaRepository extends JpaRepository<TaskAttachmentJpaEntity, UUID> {

    /**
     * Lists a task's attachments.
     *
     * @param taskId task ID
     * @return attachments, oldest first
     */
    List<TaskAttachmentJpaEntity> findByTaskIdOrderByCreatedAtAsc(UUID taskId);
}
