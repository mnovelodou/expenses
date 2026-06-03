package com.novelosoftware.expenses.dto;

import java.util.List;

/**
 * Paginated response for cursor-based pagination.
 * Does not expose a total count — clients paginate by following {@code nextCursor}
 * until it is {@code null}.
 *
 * @param <T> the type of items in the page
 */
public record CursorPageResponse<T>(
    /** The items in the current page. */
    List<T> content,
    /**
     * Opaque token to pass as {@code cursor} in the next request.
     * {@code null} when this is the last page.
     */
    String nextCursor,
    /** The number of items requested for this page. */
    int pageSize
) {}
