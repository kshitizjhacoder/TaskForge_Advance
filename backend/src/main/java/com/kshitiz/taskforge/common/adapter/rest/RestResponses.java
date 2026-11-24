package com.kshitiz.taskforge.common.adapter.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Small helpers for building HTTP responses that every controller needs.
 */
public final class RestResponses {

    private RestResponses() {
        // utility class, not meant to be instantiated
    }

    /**
     * Builds a {@code 201 Created} response whose {@code Location} header points to the new
     * resource, as REST clients expect.
     *
     * <p>Example: {@code RestResponses.created(body, "/api/tasks/{id}", task.id())} returns
     * 201 with {@code Location: http://host/api/tasks/3f2a...}.
     *
     * @param body         the created resource, returned as the response body
     * @param pathTemplate path of the new resource with {@code {placeholders}}
     * @param uriVariables values for the placeholders, in order
     * @param <T>          body type
     * @return 201 response with Location header and body
     */
    public static <T> ResponseEntity<T> created(T body, String pathTemplate, Object... uriVariables) {
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(pathTemplate)
                .buildAndExpand(uriVariables)
                .toUri();
        return ResponseEntity.created(location).body(body);
    }
}
