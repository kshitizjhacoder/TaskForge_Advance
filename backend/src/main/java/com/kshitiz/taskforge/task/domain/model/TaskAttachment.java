package com.kshitiz.taskforge.task.domain.model;

import com.kshitiz.taskforge.common.exception.BusinessRuleViolationException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A file attached to a task. Only metadata lives in the database; the bytes are kept in file
 * storage under {@link #getStorageKey()}.
 *
 * <p>The file name shown to users is cleaned: any folder path is removed (so a name like
 * "../../etc/passwd" becomes "passwd") and control characters are dropped. The storage key
 * never contains the user's file name, only IDs, so a file name can never point anywhere
 * unexpected in storage.
 */
@Getter
@Builder(builderMethodName = "restore")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TaskAttachment {

    public static final int MAX_FILE_NAME_LENGTH = 255;

    private final UUID id;
    private final UUID taskId;
    private final String fileName;
    private final String contentType;
    private final long sizeBytes;
    private final String storageKey;
    private final UUID uploadedBy;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Long version;

    /**
     * Creates the metadata for a newly uploaded file.
     *
     * @param taskId           task the file belongs to
     * @param originalFileName name sent by the client (cleaned here)
     * @param contentType      MIME type (already checked against the allowed list)
     * @param sizeBytes        file size in bytes
     * @param uploadedBy       user who uploaded it
     * @return the new, not yet saved, attachment
     * @throws BusinessRuleViolationException if the file is empty
     */
    public static TaskAttachment upload(UUID taskId, String originalFileName, String contentType, long sizeBytes,
                                        UUID uploadedBy) {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(uploadedBy, "uploadedBy");
        if (sizeBytes <= 0) {
            throw new BusinessRuleViolationException("The file is empty");
        }
        UUID id = UUID.randomUUID();
        return new TaskAttachment(id, taskId, cleanFileName(originalFileName), contentType, sizeBytes,
                "tasks/" + taskId + "/" + id, uploadedBy, null, null, null);
    }

    /**
     * Makes a client-supplied file name safe to store and show.
     *
     * @param original name from the upload, may be {@code null}
     * @return the last path segment without control characters, at most 255 characters,
     *         or "file" if nothing is left
     */
    static String cleanFileName(String original) {
        if (original == null) {
            return "file";
        }
        String name = original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        name = name.replaceAll("\\p{Cntrl}", "").trim();
        if (name.isEmpty() || name.equals(".") || name.equals("..")) {
            return "file";
        }
        return name.length() > MAX_FILE_NAME_LENGTH ? name.substring(name.length() - MAX_FILE_NAME_LENGTH) : name;
    }
}
