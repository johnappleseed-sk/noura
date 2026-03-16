package com.noura.search.controller;

import com.noura.search.common.ApiResponse;
import com.noura.search.dto.SearchSuggestionDto;
import com.noura.search.dto.TrendTagDto;
import com.noura.search.service.SearchQueryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("${app.api.version-prefix:/api/v1}/search")
public class SearchPublicController {

    private final SearchQueryService searchQueryService;

    @GetMapping("/predictive")
    public ApiResponse<List<SearchSuggestionDto>> predictive(
            @RequestParam String q,
            @RequestParam(defaultValue = "all") String scope,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Predictive search", searchQueryService.predictive(q, scope), http.getRequestURI());
    }

    @GetMapping("/trend-tags")
    public ApiResponse<List<TrendTagDto>> trendTags(HttpServletRequest http) {
        return ApiResponse.ok("Trend tags", searchQueryService.trendTags(), http.getRequestURI());
    }
}
