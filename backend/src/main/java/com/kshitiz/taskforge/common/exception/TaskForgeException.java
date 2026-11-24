package com.kshitiz.taskforge.common.exception;

/**
 * Base class for every exception TaskForge throws on purpose.
 *
 * <p>Subclasses describe <em>what</em> went wrong (not found, duplicate, rule broken).
 * {@code GlobalExceptionHandler} turns each subclass into the matching HTTP status, so
 * services never deal with HTTP codes themselves.
 */
public abstract class TaskForgeException extends RuntimeException {

    /**
     * Creates the exception with a message that is safe to show to API clients.
     *
     * @param message human-readable explanation, returned in the error response
     */
    protected TaskForgeException(String message) {
        super(message);
    }
}
