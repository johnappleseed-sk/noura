package com.noura.platform.common.api;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PaginationUtils {

    /**
     * Creates a new PaginationUtils instance.
     */
    private PaginationUtils() {
    }

    /**
     * Executes page of.
     *
     * @param page The pagination configuration.
     * @param size The size value.
     * @param sortBy The sort by value.
     * @param direction The direction value.
     * @return The result of page of.
     */
    public static Pageable pageOf(int page, int size, String sortBy, String direction) {
        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
    }
}
