package com.kshitiz.taskforge.common.exception;

/**
 * Thrown when creating something that must be unique already exists, for example a second
 * user with the same email. Mapped to HTTP 409.
 */
public class DuplicateResourceException extends TaskForgeException {

    /**
     * Creates the exception.
     *
     * @param message explanation returned to the client
     */
    public DuplicateResourceException(String message) {
        super(message);
    }
}
