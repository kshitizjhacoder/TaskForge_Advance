package com.kshitiz.taskforge.common.exception;

/**
 * Thrown when file storage (S3/MinIO) cannot be reached or refuses a request. Mapped to
 * HTTP 503, because the problem is on the server side and retrying later may work.
 */
public class StorageUnavailableException extends TaskForgeException {

    /**
     * Creates the exception, keeping the original error for the logs.
     *
     * @param message explanation returned to the client
     * @param cause   underlying storage error (logged, never sent to the client)
     */
    public StorageUnavailableException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
}
