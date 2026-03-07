package com.noura.platform.service;

import com.noura.platform.dto.product.SearchSuggestionDto;

import java.util.List;

public interface SearchService {
    /**
     * Executes predictive.
     *
     * @param q The q value.
     * @param scope The scope value.
     * @return A list of matching items.
     */
    List<SearchSuggestionDto> predictive(String q, String scope);
}
