package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.dto.pricing.*;
import com.noura.platform.service.PricingCatalogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PricingController {

    private final PricingCatalogService pricingCatalogService;

    /**
     * Creates price list.
     *
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PostMapping("${app.api.version-prefix:/api/v1}/price-lists")
    public ResponseEntity<ApiResponse<PriceListDto>> createPriceList(
            @Valid @RequestBody PriceListRequest request,
            HttpServletRequest http
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Price list created", pricingCatalogService.createPriceList(request), http.getRequestURI()));
    }

    /**
     * Retrieves price lists.
     *
     * @param http The current HTTP request used to populate response metadata.
     * @return A list of matching items.
     */
    @GetMapping("${app.api.version-prefix:/api/v1}/price-lists")
    public ApiResponse<List<PriceListDto>> priceLists(HttpServletRequest http) {
        return ApiResponse.ok("Price lists", pricingCatalogService.priceLists(), http.getRequestURI());
    }

    /**
     * Upserts price.
     *
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PostMapping("${app.api.version-prefix:/api/v1}/prices")
    public ResponseEntity<ApiResponse<PriceDto>> upsertPrice(
            @Valid @RequestBody PriceUpsertRequest request,
            HttpServletRequest http
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Price upserted", pricingCatalogService.upsertPrice(request), http.getRequestURI()));
    }

    /**
     * Quotes variant price.
     *
     * @param variantId The variant id used to locate the target record.
     * @param customerGroupId The customer group id used to locate the target record.
     * @param channelId The channel id used to locate the target record.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @GetMapping("${app.api.version-prefix:/api/v1}/prices/variants/{variantId}")
    public ApiResponse<PriceQuoteDto> quoteVariantPrice(
            @PathVariable UUID variantId,
            @RequestParam(required = false) UUID customerGroupId,
            @RequestParam(required = false) UUID channelId,
            HttpServletRequest http
    ) {
        return ApiResponse.ok(
                "Price quote",
                pricingCatalogService.quoteVariantPrice(variantId, customerGroupId, channelId),
                http.getRequestURI()
        );
    }

    /**
     * Creates promotion.
     *
     * @param request The request payload for this operation.
     * @param http The current HTTP request used to populate response metadata.
     * @return A standard API response envelope containing operation data and request metadata.
     */
    @PostMapping("${app.api.version-prefix:/api/v1}/promotions")
    public ResponseEntity<ApiResponse<PromotionDto>> createPromotion(
            @Valid @RequestBody PromotionCreateRequest request,
            HttpServletRequest http
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Promotion created", pricingCatalogService.createPromotion(request), http.getRequestURI()));
    }

    /**
     * Retrieves active promotions.
     *
     * @param http The current HTTP request used to populate response metadata.
     * @return A list of matching items.
     */
    @GetMapping("${app.api.version-prefix:/api/v1}/promotions/active")
    public ApiResponse<List<PromotionDto>> activePromotions(HttpServletRequest http) {
        return ApiResponse.ok("Active promotions", pricingCatalogService.activePromotions(), http.getRequestURI());
    }
}
