package com.kshitiz.taskforge.common.domain;

import com.kshitiz.taskforge.common.exception.InvalidRequestException;

/**
 * Framework-free description of "which page of results, sorted how".
 *
 * <p>Services and repository ports use this instead of Spring's {@code Pageable}, so the
 * domain does not depend on Spring Data. The persistence layer converts it with
 * {@code PageRequests.toPageable}.
 *
 * @param page          zero-based page number
 * @param size          number of items per page (1..{@value #MAX_SIZE})
 * @param sortBy        API name of the field to sort by, e.g. "createdAt"
 * @param sortAscending {@code true} for ascending order, {@code false} for descending
 */
public record PageQuery(int page, int size, String sortBy, boolean sortAscending) {

    /** Largest page a client may request, to protect the database. */
    public static final int MAX_SIZE = 100;

    /**
     * Validates the values when the record is created.
     *
     * @throws InvalidRequestException if the page is negative or the size is out of range
     */
    public PageQuery {
        if (page < 0) {
            throw new InvalidRequestException("Page number must be 0 or greater");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new InvalidRequestException("Page size must be between 1 and " + MAX_SIZE);
        }
    }
}
