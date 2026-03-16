package com.noura.pricing.controller;

import com.noura.pricing.common.ApiResponse;
import com.noura.pricing.dto.price.BulkPriceResolutionResponse;
import com.noura.pricing.dto.price.PriceResolutionResponse;
import com.noura.pricing.service.ProductPricingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Query endpoints for storefront and checkout price resolution.
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pricing/v1")
public class PricingQueryController {

    private final ProductPricingService productPricingService;

    /**
     * Resolves price for one product.
     *
     * @param productId product identifier
     * @param currencyCode optional currency code
     * @param storeId optional store scope
     * @param channelCode optional channel scope
     * @param at optional resolution timestamp
     * @param request servlet request
     * @return resolved price envelope
     */
    @GetMapping("/prices/products/{productId}")
    public ApiResponse<PriceResolutionResponse> getPriceByProduct(
            @PathVariable @NotNull UUID productId,
            @RequestParam(required = false) String currencyCode,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) String channelCode,
            @RequestParam(required = false) Instant at,
            HttpServletRequest request
    ) {
        PriceResolutionResponse data = productPricingService.resolveProductPrice(
                productId,
                currencyCode,
                storeId,
                channelCode,
                at
        );
        return ApiResponse.ok("Resolved product price", data, request.getRequestURI());
    }

    /**
     * Resolves prices for multiple products in one request.
     *
     * @param productIds target product IDs
     * @param currencyCode optional currency code
     * @param storeId optional store scope
     * @param channelCode optional channel scope
     * @param at optional resolution timestamp
     * @param request servlet request
     * @return bulk resolution envelope
     */
    @GetMapping("/prices/bulk")
    public ApiResponse<BulkPriceResolutionResponse> getBulkPrices(
            @RequestParam List<UUID> productIds,
            @RequestParam(required = false) String currencyCode,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) String channelCode,
            @RequestParam(required = false) Instant at,
            HttpServletRequest request
    ) {
        BulkPriceResolutionResponse data = productPricingService.resolveBulkPrices(
                productIds,
                currencyCode,
                storeId,
                channelCode,
                at
        );
        return ApiResponse.ok("Resolved bulk prices", data, request.getRequestURI());
    }

    /**
     * Returns active storefront snapshot for requested products.
     *
     * @param productIds target product IDs
     * @param currencyCode optional currency code
     * @param storeId optional store scope
     * @param channelCode optional channel scope
     * @param at optional resolution timestamp
     * @param request servlet request
     * @return active snapshot envelope
     */
    @GetMapping("/prices/snapshots/active")
    public ApiResponse<List<PriceResolutionResponse>> getActiveSnapshot(
            @RequestParam List<UUID> productIds,
            @RequestParam(required = false) String currencyCode,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) String channelCode,
            @RequestParam(required = false) Instant at,
            HttpServletRequest request
    ) {
        List<PriceResolutionResponse> data = productPricingService.activeSnapshot(
                productIds,
                currencyCode,
                storeId,
                channelCode,
                at
        );
        return ApiResponse.ok("Active pricing snapshot", data, request.getRequestURI());
    }
}

