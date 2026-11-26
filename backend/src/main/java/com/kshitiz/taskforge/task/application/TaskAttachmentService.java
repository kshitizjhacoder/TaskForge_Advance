package com.kshitiz.taskforge.task.application;

import com.kshitiz.taskforge.common.exception.ForbiddenOperationException;
import com.kshitiz.taskforge.common.exception.InvalidRequestException;
import com.kshitiz.taskforge.common.exception.ResourceNotFoundException;
import com.kshitiz.taskforge.common.exception.StorageUnavailableException;
import com.kshitiz.taskforge.common.security.CurrentUser;
import com.kshitiz.taskforge.common.storage.FileStorage;
import com.kshitiz.taskforge.task.application.dto.AttachmentDownload;
import com.kshitiz.taskforge.task.application.dto.AttachmentResponse;
import com.kshitiz.taskforge.task.application.dto.UploadedFile;
import com.kshitiz.taskforge.task.domain.model.TaskAttachment;
import com.kshitiz.taskforge.task.domain.policy.TaskAccessPolicy;
import com.kshitiz.taskforge.task.domain.port.TaskAttachmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * Use cases for files attached to tasks: upload, list, download, delete.
 *
 * <p>Metadata is stored in the database and bytes in {@link FileStorage}. Because these are
 * two separate systems, the order of steps is chosen so that a failure can only leave an
 * unused file in storage, never a database row pointing to a missing file:
 * <ul>
 *   <li>upload: store the file first, then save the row; if saving fails, delete the file;</li>
 *   <li>delete: delete the row, and remove the file only after the transaction commits.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskAttachmentService {

    private final TaskAttachmentRepository attachmentRepository;
    private final TaskService taskService;
    private final FileStorage fileStorage;
    private final AttachmentProperties properties;

    /**
     * Uploads a file and attaches it to a task. Any member of the task's project may upload.
     *
     * @param taskId task ID
     * @param file   the uploaded file
     * @param actor  the logged-in user, recorded as the uploader
     * @return the new attachment's metadata
     * @throws ResourceNotFoundException   if the task is not found or not visible
     * @throws InvalidRequestException     if the file type is not allowed, it is too large or unreadable
     * @throws StorageUnavailableException if file storage cannot be reached
     */
    @Transactional
    public AttachmentResponse upload(UUID taskId, UploadedFile file, CurrentUser actor) {
        taskService.loadAccessibleTask(taskId, actor);
        String contentType = AttachmentProperties.normalize(file.contentType());
        if (!properties.isAllowed(contentType)) {
            throw new InvalidRequestException("File type '" + contentType + "' is not allowed. Allowed types: "
                    + properties.allowedContentTypes());
        }
        if (file.sizeBytes() > properties.maxFileSize().toBytes()) {
            throw new InvalidRequestException("File is larger than the " + properties.maxFileSize().toMegabytes()
                    + " MB limit");
        }
        TaskAttachment attachment = TaskAttachment.upload(taskId, file.fileName(), contentType, file.sizeBytes(),
                actor.id());

        try (InputStream content = file.content().open()) {
            fileStorage.store(attachment.getStorageKey(), content, attachment.getSizeBytes(), contentType);
        } catch (IOException ex) {
            throw new InvalidRequestException("The uploaded file could not be read");
        }
        try {
            TaskAttachment saved = attachmentRepository.save(attachment);
            log.info("User {} attached {} ({} bytes) to task {}", actor.id(), saved.getId(), saved.getSizeBytes(),
                    taskId);
            return AttachmentResponse.from(saved);
        } catch (RuntimeException ex) {
            deleteQuietly(attachment.getStorageKey());
            throw ex;
        }
    }

    /**
     * Lists a task's attachments.
     *
     * @param taskId task ID
     * @param actor  the logged-in user
     * @return attachments, oldest first
     * @throws ResourceNotFoundException if the task is not found or not visible
     */
    public List<AttachmentResponse> listAttachments(UUID taskId, CurrentUser actor) {
        taskService.loadAccessibleTask(taskId, actor);
        return attachmentRepository.findByTaskId(taskId).stream().map(AttachmentResponse::from).toList();
    }

    /**
     * Opens an attachment for download.
     *
     * @param attachmentId attachment ID
     * @param actor        the logged-in user
     * @return metadata and an open stream of the bytes (the caller closes it)
     * @throws ResourceNotFoundException   if not found or not visible
     * @throws StorageUnavailableException if file storage cannot be reached
     */
    public AttachmentDownload download(UUID attachmentId, CurrentUser actor) {
        TaskAttachment attachment = loadAccessibleAttachment(attachmentId, actor).attachment();
        return new AttachmentDownload(attachment.getFileName(), attachment.getContentType(),
                attachment.getSizeBytes(), fileStorage.open(attachment.getStorageKey()));
    }

    /**
     * Deletes an attachment: the row now, the stored file after the transaction commits.
     *
     * @param attachmentId attachment ID
     * @param actor        the logged-in user
     * @throws ResourceNotFoundException   if not found or not visible
     * @throws ForbiddenOperationException if the user is not the uploader and may not manage the task
     */
    @Transactional
    public void deleteAttachment(UUID attachmentId, CurrentUser actor) {
        LoadedAttachment loaded = loadAccessibleAttachment(attachmentId, actor);
        TaskAccessPolicy.requireCanDeleteAttachment(loaded.attachment(), loaded.task().task(), actor,
                loaded.task().isProjectOwner(actor));
        attachmentRepository.deleteById(attachmentId);
        String storageKey = loaded.attachment().getStorageKey();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteQuietly(storageKey);
            }
        });
        log.info("User {} deleted attachment {}", actor.id(), attachmentId);
    }

    /** An attachment together with its (accessible) task. */
    private record LoadedAttachment(TaskAttachment attachment, AccessibleTask task) {
    }

    /**
     * Loads an attachment and checks the user may see its task's project.
     *
     * @param id    attachment ID
     * @param actor the logged-in user
     * @return the attachment and its task
     */
    private LoadedAttachment loadAccessibleAttachment(UUID id, CurrentUser actor) {
        TaskAttachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Attachment", id));
        return new LoadedAttachment(attachment, taskService.loadAccessibleTask(attachment.getTaskId(), actor));
    }

    /**
     * Deletes a stored file, logging instead of failing. A leftover file only wastes space,
     * so it must never break the user's request.
     *
     * @param storageKey file location
     */
    private void deleteQuietly(String storageKey) {
        try {
            fileStorage.delete(storageKey);
        } catch (StorageUnavailableException ex) {
            log.warn("Could not delete stored file {}; it is now unused", storageKey, ex.getCause());
        }
    }
}
