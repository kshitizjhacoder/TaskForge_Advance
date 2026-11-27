package com.kshitiz.taskforge.common.exception;

/**
 * Thrown when AI features are switched off, or the AI provider cannot be reached, times out
 * or is rate-limiting us. Mapped to HTTP 503: the rest of the app keeps working and the user
 * can try again later.
 */
public class AiUnavailableException extends TaskForgeException {

    /**
     * Creates the exception.
     *
     * @param message explanation returned to the client
     */
    public AiUnavailableException(String message) {
        super(message);
    }

    /**
     * Creates the exception, keeping the provider's error for the logs.
     *
     * @param message explanation returned to the client
     * @param cause   underlying error (logged, never sent to the client)
     */
    public AiUnavailableException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
}
