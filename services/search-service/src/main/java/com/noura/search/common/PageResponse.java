package com.noura.search.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Standard paginated response body used by search-service query endpoints.
 *
 * @param <T> page item type
 */
@Getter
@Builder
public class PageResponse<T> {

    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean first;
    private final boolean last;

    /**
     * Converts a Spring Data page into the API page response shape.
     *
     * @param page source page
     * @param <T> item type
     * @return API page response
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    /**
     * Frontend compatibility alias mirroring existing storefront pagination helpers.
     *
     * @return whether a next page exists
     */
    @JsonProperty("hasNext")
    public boolean hasNext() {
        return !last;
    }

    /**
     * Frontend compatibility alias mirroring existing storefront pagination helpers.
     *
     * @return whether a previous page exists
     */
    @JsonProperty("hasPrevious")
    public boolean hasPrevious() {
        return !first;
    }
}
