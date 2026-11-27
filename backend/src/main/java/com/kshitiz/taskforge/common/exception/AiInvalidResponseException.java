package com.kshitiz.taskforge.common.exception;

/**
 * Thrown when the AI answered, but the answer could not be used even after a retry (not valid
 * JSON, or nothing left after the safety checks). Mapped to HTTP 502 Bad Gateway: an upstream
 * service gave a bad response. The raw answer is never sent to the client.
 */
public class AiInvalidResponseException extends TaskForgeException {

    /**
     * Creates the exception.
     *
     * @param message explanation returned to the client
     */
    public AiInvalidResponseException(String message) {
        super(message);
    }
}
