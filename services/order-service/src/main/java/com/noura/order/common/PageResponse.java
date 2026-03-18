package com.noura.order.common;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Generic paginated response model used by Order Service list endpoints.
 *
 * @param <T> row DTO type
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    /**
     * Builds a {@link PageResponse} from a Spring Data {@link Page}.
     *
     * @param source spring page result
     * @param <T> row DTO type
     * @return mapped page response
     */
    public static <T> PageResponse<T> from(Page<T> source) {
        return new PageResponse<>(
                source.getContent(),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages(),
                source.isFirst(),
                source.isLast()
        );
    }
}

