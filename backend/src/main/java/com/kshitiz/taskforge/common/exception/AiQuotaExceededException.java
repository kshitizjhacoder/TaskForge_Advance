package com.kshitiz.taskforge.common.exception;

/**
 * Thrown when a user, or the whole application, has used up today's AI requests. Mapped to
 * HTTP 429 Too Many Requests. Limits reset at midnight UTC.
 */
public class AiQuotaExceededException extends TaskForgeException {

    /**
     * Creates the exception.
     *
     * @param message explanation returned to the client
     */
    public AiQuotaExceededException(String message) {
        super(message);
    }
}
