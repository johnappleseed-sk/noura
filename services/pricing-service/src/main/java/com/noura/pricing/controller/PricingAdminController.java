package com.noura.pricing.controller;

import com.noura.pricing.common.ApiResponse;
import com.noura.pricing.dto.price.PriceUpsertRequest;
import com.noura.pricing.dto.price.ProductPriceResponse;
import com.noura.pricing.service.ProductPricingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin mutation endpoints for product pricing records.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pricing/v1/admin")
public class PricingAdminController {

    private final ProductPricingService productPricingService;

    /**
     * Upserts a product price record using POST.
     *
     * @param requestBody upsert command
     * @param actorUserId optional actor user ID from gateway forwarding
     * @param request servlet request
     * @return upsert result envelope
     */
    @PostMapping("/prices")
    public ApiResponse<ProductPriceResponse> upsertPricePost(
            @Valid @RequestBody PriceUpsertRequest requestBody,
            @RequestHeader(value = "X-Auth-Subject", required = false) String actorUserId,
            HttpServletRequest request
    ) {
        ProductPriceResponse data = productPricingService.upsertPrice(requestBody, actorUserId);
        return ApiResponse.ok("Product price upserted", data, request.getRequestURI());
    }

    /**
     * Upserts a product price record using PUT.
     *
     * @param requestBody upsert command
     * @param actorUserId optional actor user ID from gateway forwarding
     * @param request servlet request
     * @return upsert result envelope
     */
    @PutMapping("/prices")
    public ApiResponse<ProductPriceResponse> upsertPricePut(
            @Valid @RequestBody PriceUpsertRequest requestBody,
            @RequestHeader(value = "X-Auth-Subject", required = false) String actorUserId,
            HttpServletRequest request
    ) {
        ProductPriceResponse data = productPricingService.upsertPrice(requestBody, actorUserId);
        return ApiResponse.ok("Product price upserted", data, request.getRequestURI());
    }
}

