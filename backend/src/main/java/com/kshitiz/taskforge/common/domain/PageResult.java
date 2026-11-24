package com.kshitiz.taskforge.common.domain;

import java.util.List;
import java.util.function.Function;

/**
 * One page of results plus the numbers a client needs to show pagination controls.
 *
 * <p>This record is also the JSON shape of every paged API response, so all list
 * endpoints look the same.
 *
 * @param content       items on this page
 * @param page          zero-based page number
 * @param size          requested page size
 * @param totalElements number of items across all pages
 * @param totalPages    number of pages
 * @param <T>           item type
 */
public record PageResult<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    /**
     * Converts every item on the page, keeping the paging numbers.
     *
     * <p>Typical use: turn a page of domain objects into a page of response DTOs.
     *
     * @param mapper conversion applied to each item
     * @param <R>    new item type
     * @return a new page with converted items
     */
    public <R> PageResult<R> map(Function<? super T, ? extends R> mapper) {
        List<R> mapped = content.stream().<R>map(mapper).toList();
        return new PageResult<>(mapped, page, size, totalElements, totalPages);
    }
}
