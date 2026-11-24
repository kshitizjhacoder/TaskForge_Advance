package com.kshitiz.taskforge.common.exception;

/**
 * Thrown when a logged-in user tries something they are not allowed to do, for example
 * editing another user's comment. Mapped to HTTP 403.
 */
public class ForbiddenOperationException extends TaskForgeException {

    /**
     * Creates the exception.
     *
     * @param message explanation returned to the client
     */
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
