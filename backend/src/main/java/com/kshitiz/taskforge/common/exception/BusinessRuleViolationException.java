package com.kshitiz.taskforge.common.exception;

/**
 * Thrown by domain objects when a request is well-formed but breaks a business rule,
 * for example moving a task from TO_DO straight to DONE, or an end date before the start
 * date. Mapped to HTTP 422.
 */
public class BusinessRuleViolationException extends TaskForgeException {

    /**
     * Creates the exception.
     *
     * @param message explanation of the broken rule, returned to the client
     */
    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
