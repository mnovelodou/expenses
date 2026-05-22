package com.novelosoftware.expenses.dto;

import java.util.List;

/**
 * Generic paginated response wrapper.
 *
 * @param <T> the type of items in the page
 */
public record PageResponse<T>(
    /** The items in the current page. */
    List<T> content,
    /** The current page number, zero-based. */
    int page,
    /** The number of items per page. */
    int size,
    /** The total number of items across all pages. */
    long totalElements,
    /** The total number of pages. */
    int totalPages
) {
    /**
     * Constructs a PageResponse, computing totalPages from totalElements and size.
     *
     * @param content       items in the current page
     * @param page          zero-based page number
     * @param size          page size
     * @param totalElements total number of matching items
     */
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }
}
