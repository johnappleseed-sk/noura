package com.noura.search.service;

import com.noura.search.dto.SearchSuggestionDto;
import com.noura.search.dto.TrendTagDto;

import java.util.List;

public interface SearchQueryService {
    List<SearchSuggestionDto> predictive(String q, String scope);

    List<TrendTagDto> trendTags();
}
