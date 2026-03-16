package com.noura.catalog.controller;

import com.noura.catalog.common.ApiResponse;
import com.noura.catalog.dto.product.SearchSuggestionDto;
import com.noura.catalog.dto.product.TrendTagDto;
import com.noura.catalog.service.CatalogSearchService;
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
public class CatalogSearchController {

    private final CatalogSearchService catalogSearchService;

    @GetMapping("/predictive")
    public ApiResponse<List<SearchSuggestionDto>> predictive(
            @RequestParam String q,
            @RequestParam(defaultValue = "all") String scope,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Predictive search", catalogSearchService.predictive(q, scope), http.getRequestURI());
    }

    @GetMapping("/trend-tags")
    public ApiResponse<List<TrendTagDto>> trendTags(HttpServletRequest http) {
        return ApiResponse.ok("Trend tags", catalogSearchService.trendTags(), http.getRequestURI());
    }
}
