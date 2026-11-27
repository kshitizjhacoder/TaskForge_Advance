package com.kshitiz.taskforge.common.adapter.rest;

import com.kshitiz.taskforge.common.exception.AiInvalidResponseException;
import com.kshitiz.taskforge.common.exception.AiQuotaExceededException;
import com.kshitiz.taskforge.common.exception.AiUnavailableException;
import com.kshitiz.taskforge.common.exception.AuthenticationFailedException;
import com.kshitiz.taskforge.common.exception.BusinessRuleViolationException;
import com.kshitiz.taskforge.common.exception.DuplicateResourceException;
import com.kshitiz.taskforge.common.exception.ForbiddenOperationException;
import com.kshitiz.taskforge.common.exception.InvalidRequestException;
import com.kshitiz.taskforge.common.exception.ResourceNotFoundException;
import com.kshitiz.taskforge.common.exception.StorageUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns every exception into an RFC 7807 {@link ProblemDetail} JSON response, so clients
 * always get the same error shape:
 *
 * <pre>
 * { "type": "about:blank", "title": "Not Found", "status": 404,
 *   "detail": "Task not found: 3f2a...", "instance": "/api/tasks/3f2a..." }
 * </pre>
 *
 * <p>The parent class {@link ResponseEntityExceptionHandler} already covers Spring MVC
 * errors (malformed JSON, a wrong type in a path variable, unsupported method, ...).
 * This class adds TaskForge's own exceptions, validation details, database conflicts and a
 * safe fallback for anything unexpected.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * A requested record does not exist → 404 Not Found.
     *
     * @param ex the exception thrown by a service
     * @return problem detail with the exception message
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Something that must be unique already exists → 409 Conflict.
     *
     * @param ex the exception thrown by a service
     * @return problem detail with the exception message
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicate(DuplicateResourceException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * A business rule was broken (e.g. an invalid status change) → 422 Unprocessable Entity.
     *
     * @param ex the exception thrown by a domain object
     * @return problem detail with the rule that was broken
     */
    @ExceptionHandler(BusinessRuleViolationException.class)
    public ProblemDetail handleBusinessRule(BusinessRuleViolationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    /**
     * A request parameter has an unacceptable value (e.g. unknown sort field) → 400.
     *
     * @param ex the exception thrown while reading the request
     * @return problem detail with the reason
     */
    @ExceptionHandler(InvalidRequestException.class)
    public ProblemDetail handleInvalidRequest(InvalidRequestException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Wrong credentials or an invalid token → 401 Unauthorized.
     *
     * @param ex the exception thrown by the auth service
     * @return problem detail with a deliberately vague message
     */
    @ExceptionHandler(AuthenticationFailedException.class)
    public ProblemDetail handleAuthenticationFailed(AuthenticationFailedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    /**
     * The user is logged in but not allowed to do this → 403 Forbidden. Covers both our own
     * ownership checks ({@link ForbiddenOperationException}) and Spring's role checks from
     * {@code @PreAuthorize} ({@link AccessDeniedException}).
     *
     * @param ex the exception thrown by a service or by method security
     * @return problem detail explaining the refusal
     */
    @ExceptionHandler({ForbiddenOperationException.class, AccessDeniedException.class})
    public ProblemDetail handleForbidden(Exception ex) {
        String detail = ex instanceof ForbiddenOperationException
                ? ex.getMessage()
                : "You do not have permission to perform this action.";
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, detail);
    }

    /**
     * File storage (S3/MinIO) is unreachable → 503 Service Unavailable. The storage error is
     * logged; the client only learns that it should retry later.
     *
     * @param ex the exception thrown by the storage adapter
     * @return problem detail asking the client to retry later
     */
    @ExceptionHandler(StorageUnavailableException.class)
    public ProblemDetail handleStorageUnavailable(StorageUnavailableException ex) {
        log.error("File storage error", ex.getCause());
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    /**
     * AI is switched off or the provider is unreachable → 503 Service Unavailable.
     *
     * @param ex the exception thrown by the AI adapter
     * @return problem detail asking the client to try later
     */
    @ExceptionHandler(AiUnavailableException.class)
    public ProblemDetail handleAiUnavailable(AiUnavailableException ex) {
        if (ex.getCause() != null) {
            log.warn("AI provider error: {}", ex.getCause().toString());
        }
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    /**
     * Today's AI allowance is used up → 429 Too Many Requests.
     *
     * @param ex the exception thrown by the AI quota check
     * @return problem detail explaining the limit
     */
    @ExceptionHandler(AiQuotaExceededException.class)
    public ProblemDetail handleAiQuota(AiQuotaExceededException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }

    /**
     * The AI answered with something unusable → 502 Bad Gateway.
     *
     * @param ex the exception thrown after validating the AI's answer
     * @return problem detail with a generic message
     */
    @ExceptionHandler(AiInvalidResponseException.class)
    public ProblemDetail handleAiInvalidResponse(AiInvalidResponseException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    /**
     * Someone else updated the same record after this client read it → 409 Conflict.
     * The client should reload and try again.
     *
     * @param ex exception raised by JPA's version check
     * @return problem detail asking the client to reload
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "This record was changed by someone else. Reload it and try again.");
    }

    /**
     * The database rejected a write because of a constraint (unique key, foreign key, check)
     * that the service layer did not catch first, for example two requests creating the
     * same email at the same moment → 409 Conflict. The SQL error is logged, never returned.
     *
     * @param ex exception raised by the database driver
     * @return generic conflict problem detail
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Database constraint violation: {}", ex.getMostSpecificCause().getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "The request conflicts with existing data.");
    }

    /**
     * Last-resort handler for bugs and unexpected failures → 500. The full stack trace is
     * logged for developers; the client only sees a generic message so internal details
     * never leak.
     *
     * @param ex any exception not handled above
     * @return generic internal-error problem detail
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong on our side. Please try again later.");
    }

    /**
     * Request body failed Bean Validation ({@code @NotBlank}, {@code @Size}, ...) → 400.
     * Adds an {@code errors} map of field name → message so the frontend can highlight
     * each invalid field.
     *
     * @param ex      validation exception with every failed field
     * @param headers response headers prepared by Spring
     * @param status  status chosen by Spring (400)
     * @param request current request
     * @return 400 response with per-field errors
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(@NonNull MethodArgumentNotValidException ex,
                                                                  @NonNull HttpHeaders headers,
                                                                  @NonNull HttpStatusCode status,
                                                                  @NonNull WebRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "One or more fields are invalid.");
        problem.setProperty("errors", fieldErrors);
        return ResponseEntity.badRequest().body(problem);
    }
}
