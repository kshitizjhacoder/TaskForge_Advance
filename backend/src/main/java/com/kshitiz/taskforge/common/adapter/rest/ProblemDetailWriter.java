package com.kshitiz.taskforge.common.adapter.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

/**
 * Writes an RFC 7807 {@link ProblemDetail} JSON error straight to the HTTP response.
 *
 * <p>Needed for errors raised inside servlet filters (missing token, rate limit), which happen
 * before Spring MVC and so never reach {@link GlobalExceptionHandler}. Using this keeps those
 * errors in exactly the same JSON shape as all other errors.
 */
@Component
@RequiredArgsConstructor
public class ProblemDetailWriter {

    private final ObjectMapper objectMapper;

    /**
     * Writes the error response.
     *
     * @param request  current request (its path becomes the problem's "instance")
     * @param response response to write to
     * @param status   HTTP status to send
     * @param detail   human-readable explanation
     * @throws IOException if the response cannot be written
     */
    public void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String detail)
            throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setInstance(URI.create(request.getRequestURI()));
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
