package com.kshitiz.taskforge.common.exception;

/**
 * Thrown when a request parameter has a value the API does not accept, for example an
 * unknown sort field or a page size above the limit. Mapped to HTTP 400.
 *
 * <p>Field validation on request bodies ({@code @NotBlank}, {@code @Size}, ...) is handled
 * separately by Bean Validation; this exception covers checks done in code.
 */
public class InvalidRequestException extends TaskForgeException {

    /**
     * Creates the exception.
     *
     * @param message explanation returned to the client
     */
    public InvalidRequestException(String message) {
        super(message);
    }
}
