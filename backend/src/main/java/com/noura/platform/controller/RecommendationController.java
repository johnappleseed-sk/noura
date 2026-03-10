package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.dto.recommendation.ProductRecommendationResponse;
import com.noura.platform.dto.recommendation.RecommendationProductDto;
import com.noura.platform.service.RecommendationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("${app.api.version-prefix:/api/v1}/recommendations/product/{productId}")
    public ApiResponse<ProductRecommendationResponse> productRecommendations(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "6") int limit,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Product recommendations", recommendationService.getProductRecommendations(productId, limit), http.getRequestURI());
    }

    @GetMapping("${app.api.version-prefix:/api/v1}/recommendations/trending")
    public ApiResponse<List<RecommendationProductDto>> trendingRecommendations(
            @RequestParam(defaultValue = "8") int limit,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Trending recommendations", recommendationService.getTrendingRecommendations(limit), http.getRequestURI());
    }

    @GetMapping("${app.api.version-prefix:/api/v1}/recommendations/best-sellers")
    public ApiResponse<List<RecommendationProductDto>> bestSellerRecommendations(
            @RequestParam(defaultValue = "8") int limit,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Best-seller recommendations", recommendationService.getBestSellerRecommendations(limit), http.getRequestURI());
    }

    @GetMapping("${app.api.version-prefix:/api/v1}/recommendations/deals")
    public ApiResponse<List<RecommendationProductDto>> dealRecommendations(
            @RequestParam(defaultValue = "8") int limit,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Deal recommendations", recommendationService.getDealRecommendations(limit), http.getRequestURI());
    }

    @GetMapping("${app.api.version-prefix:/api/v1}/recommendations/personalized")
    public ApiResponse<List<RecommendationProductDto>> personalizedRecommendations(
            @RequestParam(defaultValue = "8") int limit,
            Authentication authentication,
            HttpServletRequest http
    ) {
        String customerRef = authentication == null ? null : authentication.getName();
        return ApiResponse.ok(
                "Personalized recommendations",
                recommendationService.getPersonalizedRecommendations(customerRef, limit),
                http.getRequestURI()
        );
    }

    @GetMapping("${app.api.version-prefix:/api/v1}/recommendations/cross-sell")
    public ApiResponse<List<RecommendationProductDto>> crossSellRecommendations(
            @RequestParam(defaultValue = "8") int limit,
            Authentication authentication,
            HttpServletRequest http
    ) {
        String customerRef = authentication == null ? null : authentication.getName();
        return ApiResponse.ok(
                "Cross-sell recommendations",
                recommendationService.getCrossSellRecommendations(customerRef, limit),
                http.getRequestURI()
        );
    }

    @GetMapping("${app.api.version-prefix:/api/v1}/products/{productId}/related")
    public ApiResponse<List<RecommendationProductDto>> relatedProducts(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "6") int limit,
            HttpServletRequest http
    ) {
        return ApiResponse.ok(
                "Related products",
                recommendationService.getProductRecommendations(productId, limit).relatedProducts(),
                http.getRequestURI()
        );
    }

    @GetMapping("${app.api.version-prefix:/api/v1}/products/{productId}/frequently-bought-together")
    public ApiResponse<List<RecommendationProductDto>> frequentlyBoughtTogether(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "6") int limit,
            HttpServletRequest http
    ) {
        return ApiResponse.ok(
                "Frequently bought together",
                recommendationService.getProductRecommendations(productId, limit).frequentlyBoughtTogether(),
                http.getRequestURI()
        );
    }
}
