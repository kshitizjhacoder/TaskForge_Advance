package com.kshitiz.taskforge.common.adapter.rest;

import com.kshitiz.taskforge.common.domain.PageQuery;
import com.kshitiz.taskforge.common.exception.InvalidRequestException;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Paging query parameters shared by every list endpoint:
 * {@code ?page=0&size=20&sort=createdAt&direction=desc}.
 *
 * <p>Controllers receive this record (Spring fills it from the query string) and call
 * {@link #toPageQuery(String)}, so defaults and checks are written once.
 *
 * @param page      zero-based page number (default 0)
 * @param size      items per page (default 20, max 100)
 * @param sort      field to sort by; allowed values depend on the endpoint
 * @param direction "asc" or "desc" (default "desc")
 */
public record PageParams(
        @Schema(description = "Zero-based page number", defaultValue = "0") Integer page,
        @Schema(description = "Items per page (max 100)", defaultValue = "20") Integer size,
        @Schema(description = "Field to sort by") String sort,
        @Schema(description = "asc or desc", defaultValue = "desc") String direction) {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    /**
     * Applies defaults and converts the parameters into a {@link PageQuery}.
     *
     * @param defaultSort sort field used when the client sends none
     * @return validated page query
     * @throws InvalidRequestException if direction is not "asc"/"desc" or paging values are out of range
     */
    public PageQuery toPageQuery(String defaultSort) {
        String dir = direction == null ? "desc" : direction.toLowerCase();
        if (!dir.equals("asc") && !dir.equals("desc")) {
            throw new InvalidRequestException("direction must be 'asc' or 'desc'");
        }
        return new PageQuery(
                page == null ? DEFAULT_PAGE : page,
                size == null ? DEFAULT_SIZE : size,
                sort == null || sort.isBlank() ? defaultSort : sort,
                dir.equals("asc"));
    }
}
