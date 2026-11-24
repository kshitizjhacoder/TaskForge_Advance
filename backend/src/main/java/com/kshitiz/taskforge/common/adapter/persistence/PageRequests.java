package com.kshitiz.taskforge.common.adapter.persistence;

import com.kshitiz.taskforge.common.domain.PageQuery;
import com.kshitiz.taskforge.common.domain.PageResult;
import com.kshitiz.taskforge.common.exception.InvalidRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;
import java.util.function.Function;

/**
 * Converts between TaskForge's framework-free paging types ({@link PageQuery},
 * {@link PageResult}) and Spring Data's ({@link Pageable}, {@link Page}).
 *
 * <p>Every persistence adapter that returns pages uses these two methods, so paging and
 * sort validation are written once.
 */
public final class PageRequests {

    private PageRequests() {
        // utility class, not meant to be instantiated
    }

    /**
     * Builds a Spring Data {@link Pageable} from a {@link PageQuery}.
     *
     * <p>Only fields in {@code sortableFields} may be used for sorting. This stops clients
     * from sorting by unindexed or internal columns and turns a typo into a clear 400 error
     * instead of a 500.
     *
     * @param query          page number, size and sort requested by the client
     * @param sortableFields allowed sort names, mapped from API name to entity property name
     * @return the equivalent Spring Data page request
     * @throws InvalidRequestException if the sort field is not allowed
     */
    public static Pageable toPageable(PageQuery query, Map<String, String> sortableFields) {
        if (query.sortBy() == null || query.sortBy().isBlank()) {
            return PageRequest.of(query.page(), query.size());
        }
        String property = sortableFields.get(query.sortBy());
        if (property == null) {
            throw new InvalidRequestException(
                    "Cannot sort by '" + query.sortBy() + "'. Allowed: " + sortableFields.keySet());
        }
        Sort.Direction direction = query.sortAscending() ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(query.page(), query.size(), Sort.by(direction, property));
    }

    /**
     * Converts a Spring Data {@link Page} of entities into a {@link PageResult} of domain
     * objects.
     *
     * @param page   page returned by a Spring Data repository
     * @param mapper conversion from entity to domain object
     * @param <E>    entity type
     * @param <D>    domain type
     * @return the same page with converted items
     */
    public static <E, D> PageResult<D> toPageResult(Page<E> page, Function<? super E, ? extends D> mapper) {
        return new PageResult<>(
                page.getContent().stream().<D>map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
