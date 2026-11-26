package com.kshitiz.taskforge.task.application.dto;

import com.kshitiz.taskforge.task.domain.model.TaskAttachment;

import java.time.Instant;
import java.util.UUID;

/**
 * JSON shape of an attachment. The storage location is deliberately not exposed; files are
 * downloaded through {@code GET /api/attachments/{id}/content}.
 *
 * @param id          attachment ID
 * @param taskId      task it belongs to
 * @param fileName    cleaned original file name
 * @param contentType MIME type
 * @param sizeBytes   size in bytes
 * @param uploadedBy  uploader's user ID
 * @param createdAt   upload time
 */
public record AttachmentResponse(UUID id, UUID taskId, String fileName, String contentType, long sizeBytes,
                                 UUID uploadedBy, Instant createdAt) {

    /**
     * Converts a domain attachment into its API representation.
     *
     * @param attachment domain object
     * @return response DTO
     */
    public static AttachmentResponse from(TaskAttachment attachment) {
        return new AttachmentResponse(attachment.getId(), attachment.getTaskId(), attachment.getFileName(),
                attachment.getContentType(), attachment.getSizeBytes(), attachment.getUploadedBy(),
                attachment.getCreatedAt());
    }
}
