package com.noura.catalog.controller;

import com.noura.catalog.common.ApiResponse;
import com.noura.catalog.dto.product.ProductDto;
import com.noura.catalog.service.CatalogQueryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Storefront recommendation compatibility controller backed by the catalog query service.
 *
 * <p>The extracted platform does not yet have a dedicated recommendation service in active use,
 * so these endpoints preserve the legacy frontend contracts with deterministic catalog-based
 * heuristics until a richer recommendation stack is introduced.</p>
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("${app.api.version-prefix:/api/v1}")
public class CatalogRecommendationController {

    private static final String SUBJECT_HEADER = "X-Auth-Subject";

    private final CatalogQueryService catalogQueryService;

    /**
     * Returns best-selling products for homepage and merchandising rails.
     *
     * @param limit maximum number of items to return
     * @param request current request
     * @return recommendation list
     */
    @GetMapping("/recommendations/best-sellers")
    public ApiResponse<List<ProductDto>> bestSellers(
            @RequestParam(defaultValue = "8") int limit,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                "Best-seller recommendations",
                catalogQueryService.bestSellerRecommendations(limit),
                request.getRequestURI()
        );
    }

    /**
     * Returns trending products for storefront discovery rails.
     *
     * @param limit maximum number of items to return
     * @param request current request
     * @return recommendation list
     */
    @GetMapping("/recommendations/trending")
    public ApiResponse<List<ProductDto>> trending(
            @RequestParam(defaultValue = "8") int limit,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                "Trending recommendations",
                catalogQueryService.trendingRecommendations(limit),
                request.getRequestURI()
        );
    }

    /**
     * Returns flash-sale and deal-oriented products.
     *
     * @param limit maximum number of items to return
     * @param request current request
     * @return recommendation list
     */
    @GetMapping("/recommendations/deals")
    public ApiResponse<List<ProductDto>> deals(
            @RequestParam(defaultValue = "8") int limit,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                "Deal recommendations",
                catalogQueryService.dealRecommendations(limit),
                request.getRequestURI()
        );
    }

    /**
     * Returns a deterministic mock-AI rail for diagnostics and control-center testing.
     *
     * @param customerRef optional gateway-forwarded customer subject
     * @param limit maximum number of items to return
     * @param request current request
     * @return recommendation list
     */
    @GetMapping("/recommendations/mock-ai")
    public ApiResponse<List<ProductDto>> mockAi(
            @RequestHeader(value = SUBJECT_HEADER, required = false) String customerRef,
            @RequestParam(defaultValue = "8") int limit,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                "Mock AI recommendations",
                catalogQueryService.personalizedRecommendations(customerRef, limit),
                request.getRequestURI()
        );
    }

    /**
     * Returns deterministic personalized recommendations using the forwarded customer subject when available.
     *
     * @param customerRef optional gateway-forwarded customer subject
     * @param limit maximum number of items to return
     * @param request current request
     * @return recommendation list
     */
    @GetMapping("/recommendations/personalized")
    public ApiResponse<List<ProductDto>> personalized(
            @RequestHeader(value = SUBJECT_HEADER, required = false) String customerRef,
            @RequestParam(defaultValue = "8") int limit,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                "Personalized recommendations",
                catalogQueryService.personalizedRecommendations(customerRef, limit),
                request.getRequestURI()
        );
    }

    /**
     * Returns deterministic cross-sell suggestions using the forwarded customer subject when available.
     *
     * @param customerRef optional gateway-forwarded customer subject
     * @param limit maximum number of items to return
     * @param request current request
     * @return recommendation list
     */
    @GetMapping("/recommendations/cross-sell")
    public ApiResponse<List<ProductDto>> crossSell(
            @RequestHeader(value = SUBJECT_HEADER, required = false) String customerRef,
            @RequestParam(defaultValue = "8") int limit,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                "Cross-sell recommendations",
                catalogQueryService.crossSellRecommendations(customerRef, limit),
                request.getRequestURI()
        );
    }

    /**
     * Returns related products for one product detail page.
     *
     * @param productId anchor product identifier
     * @param limit maximum number of items to return
     * @param request current request
     * @return related product list
     */
    @GetMapping("/products/{productId}/related")
    public ApiResponse<List<ProductDto>> relatedProducts(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "6") int limit,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                "Related products",
                catalogQueryService.relatedProducts(productId, limit),
                request.getRequestURI()
        );
    }

    /**
     * Returns heuristic "frequently bought together" suggestions for one product detail page.
     *
     * @param productId anchor product identifier
     * @param limit maximum number of items to return
     * @param request current request
     * @return add-on recommendation list
     */
    @GetMapping("/products/{productId}/frequently-bought-together")
    public ApiResponse<List<ProductDto>> frequentlyBoughtTogether(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "6") int limit,
            HttpServletRequest request
    ) {
        return ApiResponse.ok(
                "Frequently bought together",
                catalogQueryService.frequentlyBoughtTogether(productId, limit),
                request.getRequestURI()
        );
    }
}
