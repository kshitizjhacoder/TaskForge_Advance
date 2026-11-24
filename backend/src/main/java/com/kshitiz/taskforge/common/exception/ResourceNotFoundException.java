package com.kshitiz.taskforge.common.exception;

/**
 * Thrown when a requested record does not exist (or was soft-deleted). Mapped to HTTP 404.
 */
public class ResourceNotFoundException extends TaskForgeException {

    /**
     * Creates the exception with a ready-made message.
     *
     * @param message explanation returned to the client
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Builds the standard "not found" message for a resource looked up by a key.
     *
     * <p>Example: {@code ResourceNotFoundException.of("Task", id)} gives
     * "Task not found: 3f2a...".
     *
     * @param resource name of the resource type, e.g. "Task"
     * @param key      the identifier that was searched for
     * @return the exception, ready to throw
     */
    public static ResourceNotFoundException of(String resource, Object key) {
        return new ResourceNotFoundException(resource + " not found: " + key);
    }
}
