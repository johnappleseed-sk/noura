package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.dto.product.AiRecommendationResponse;
import com.noura.platform.dto.product.ProductDto;
import com.noura.platform.service.RecommendationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("${app.api.version-prefix:/api/v1}/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    /**
     * Executes mock ai.
     *
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @GetMapping("/mock-ai")
    public ApiResponse<AiRecommendationResponse> mockAi(HttpServletRequest http) {
        List<ProductDto> products = recommendationService.personalized();
        AiRecommendationResponse payload = new AiRecommendationResponse(
                "MockRanker-v1",
                "Ranked by recent popularity, category affinity, and catalog quality signals.",
                Instant.now(),
                products
        );
        return ApiResponse.ok("AI recommendations", payload, http.getRequestURI());
    }

    /**
     * Executes personalized.
     *
     * @param http The current HTTP request used to populate response metadata.
     * @return A list of matching items.
     */
    @GetMapping("/personalized")
    public ApiResponse<List<ProductDto>> personalized(HttpServletRequest http) {
        return ApiResponse.ok("Personalized recommendations", recommendationService.personalized(), http.getRequestURI());
    }

    /**
     * Executes cross sell.
     *
     * @param http The current HTTP request used to populate response metadata.
     * @return A list of matching items.
     */
    @GetMapping("/cross-sell")
    public ApiResponse<List<ProductDto>> crossSell(HttpServletRequest http) {
        return ApiResponse.ok("Cross sell recommendations", recommendationService.crossSell(), http.getRequestURI());
    }

    /**
     * Executes best sellers.
     *
     * @param http The current HTTP request used to populate response metadata.
     * @return A list of matching items.
     */
    @GetMapping("/best-sellers")
    public ApiResponse<List<ProductDto>> bestSellers(HttpServletRequest http) {
        return ApiResponse.ok("Best sellers", recommendationService.bestSellers(), http.getRequestURI());
    }

    /**
     * Executes trending.
     *
     * @param http The current HTTP request used to populate response metadata.
     * @return A list of matching items.
     */
    @GetMapping("/trending")
    public ApiResponse<List<ProductDto>> trending(HttpServletRequest http) {
        return ApiResponse.ok("Trending products", recommendationService.trending(), http.getRequestURI());
    }

    /**
     * Executes deals.
     *
     * @param http The current HTTP request used to populate response metadata.
     * @return A list of matching items.
     */
    @GetMapping("/deals")
    public ApiResponse<List<ProductDto>> deals(HttpServletRequest http) {
        return ApiResponse.ok("Deals", recommendationService.deals(), http.getRequestURI());
    }
}
