package com.kshitiz.taskforge.task.domain.port;

import com.kshitiz.taskforge.task.domain.model.TaskAttachment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port (interface) for attachment metadata. Implemented by
 * {@code TaskAttachmentPersistenceAdapter}. File contents are handled by
 * {@code FileStorage}, not here.
 */
public interface TaskAttachmentRepository {

    /**
     * Stores the metadata of a new attachment.
     *
     * @param attachment attachment to store
     * @return the stored attachment with timestamps filled in
     */
    TaskAttachment save(TaskAttachment attachment);

    /**
     * Finds an attachment by ID.
     *
     * @param id attachment ID
     * @return the attachment, or empty if not found
     */
    Optional<TaskAttachment> findById(UUID id);

    /**
     * Lists a task's attachments, oldest first.
     *
     * @param taskId task ID
     * @return attachments
     */
    List<TaskAttachment> findByTaskId(UUID taskId);

    /**
     * Deletes an attachment's metadata row.
     *
     * @param id attachment ID
     */
    void deleteById(UUID id);
}
