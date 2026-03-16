package com.noura.catalog.service;

import com.noura.catalog.dto.product.SearchSuggestionDto;
import com.noura.catalog.dto.product.TrendTagDto;

import java.util.List;

public interface CatalogSearchService {
    List<SearchSuggestionDto> predictive(String q, String scope);

    List<TrendTagDto> trendTags();
}
