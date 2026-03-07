package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.dto.product.SearchSuggestionDto;
import com.noura.platform.dto.product.TrendTagDto;
import com.noura.platform.service.ProductService;
import com.noura.platform.service.SearchService;
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
public class SearchController {

    private final SearchService searchService;
    private final ProductService productService;

    /**
     * Executes predictive.
     *
     * @param q The q value.
     * @param scope The scope value.
     * @param http The current HTTP request used to populate response metadata.
     * @return A list of matching items.
     */
    @GetMapping("/predictive")
    public ApiResponse<List<SearchSuggestionDto>> predictive(
            @RequestParam String q,
            @RequestParam(defaultValue = "all") String scope,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Predictive search", searchService.predictive(q, scope), http.getRequestURI());
    }

    /**
     * Executes trend tags.
     *
     * @param http The current HTTP request used to populate response metadata.
     * @return A list of matching items.
     */
    @GetMapping("/trend-tags")
    public ApiResponse<List<TrendTagDto>> trendTags(HttpServletRequest http) {
        return ApiResponse.ok("Trend search tags", productService.trendTags(), http.getRequestURI());
    }
}
