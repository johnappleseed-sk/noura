package com.noura.search.controller;

import com.noura.search.common.ApiResponse;
import com.noura.search.common.PageResponse;
import com.noura.search.dto.ProductSearchHitDto;
import com.noura.search.dto.SearchSuggestionDto;
import com.noura.search.dto.TrendTagDto;
import com.noura.search.service.SearchQueryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Public search/discovery controller.
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("${app.api.version-prefix:/api/v1}/search")
public class SearchPublicController {

    private final SearchQueryService searchQueryService;

    /**
     * Queries indexed product documents through the canonical search-service contract.
     *
     * @param q optional free-text query
     * @param keyword optional alias for free-text query
     * @param categoryId optional category filter
     * @param brandId optional brand filter
     * @param page zero-based page index
     * @param size page size
     * @param http current request
     * @return paged product hits
     */
    @GetMapping("/products")
    public ApiResponse<PageResponse<ProductSearchHitDto>> searchProducts(
            @RequestParam(required = false, name = "q") String q,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID brandId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            HttpServletRequest http
    ) {
        PageResponse<ProductSearchHitDto> data = PageResponse.from(
                searchQueryService.searchProducts(coalesceKeyword(keyword, q), categoryId, brandId, page, size)
        );
        return ApiResponse.ok("Product search results", data, http.getRequestURI());
    }

    /**
     * Returns predictive suggestions backed by the indexed search projection.
     *
     * @param q query text
     * @param scope suggestion scope
     * @param http current request
     * @return suggestion list
     */
    @GetMapping("/predictive")
    public ApiResponse<List<SearchSuggestionDto>> predictive(
            @RequestParam String q,
            @RequestParam(defaultValue = "all") String scope,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Predictive search", searchQueryService.predictive(q, scope), http.getRequestURI());
    }

    /**
     * Returns trend tags derived from the indexed search projection.
     *
     * @param http current request
     * @return trend tags
     */
    @GetMapping("/trend-tags")
    public ApiResponse<List<TrendTagDto>> trendTags(HttpServletRequest http) {
        return ApiResponse.ok("Trend tags", searchQueryService.trendTags(), http.getRequestURI());
    }

    /**
     * Coalesces compatibility query aliases into one keyword value.
     *
     * @param keyword explicit keyword parameter
     * @param q legacy `q` parameter
     * @return resolved query
     */
    private String coalesceKeyword(String keyword, String q) {
        if (keyword != null && !keyword.isBlank()) {
            return keyword;
        }
        return q;
    }
}
