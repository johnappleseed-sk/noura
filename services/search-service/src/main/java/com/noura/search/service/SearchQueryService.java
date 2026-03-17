package com.noura.search.service;

import com.noura.search.dto.ProductSearchHitDto;
import com.noura.search.dto.SearchSuggestionDto;
import com.noura.search.dto.TrendTagDto;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

/**
 * Query-facing search service for search/discovery APIs.
 */
public interface SearchQueryService {
    /**
     * Searches indexed product documents using the canonical search-service contract.
     *
     * @param query free-text query
     * @param categoryId optional category filter
     * @param brandId optional brand filter
     * @param page zero-based page index
     * @param size page size
     * @return paged product hits
     */
    Page<ProductSearchHitDto> searchProducts(String query, UUID categoryId, UUID brandId, int page, int size);

    /**
     * Returns predictive suggestions for the supplied query text.
     *
     * @param q free-text query
     * @param scope suggestion scope
     * @return suggestion list
     */
    List<SearchSuggestionDto> predictive(String q, String scope);

    /**
     * Returns current trend tags derived from indexed product documents.
     *
     * @return trend tag list
     */
    List<TrendTagDto> trendTags();
}
