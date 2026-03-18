package com.noura.pricing.controller;

import com.noura.pricing.common.ApiResponse;
import com.noura.pricing.domain.entity.LegacyPriceList;
import com.noura.pricing.dto.price.LegacyPriceListRequest;
import com.noura.pricing.dto.price.LegacyPriceListResponse;
import com.noura.pricing.dto.price.LegacyVariantPriceQuoteResponse;
import com.noura.pricing.dto.price.LegacyVariantPriceResponse;
import com.noura.pricing.dto.price.LegacyVariantPriceUpsertRequest;
import com.noura.pricing.dto.price.PriceResolutionResponse;
import com.noura.pricing.dto.price.PriceUpsertRequest;
import com.noura.pricing.dto.price.ProductPriceResponse;
import com.noura.pricing.exception.NotFoundException;
import com.noura.pricing.integration.client.CatalogVariantLookupClient;
import com.noura.pricing.repository.LegacyPriceListRepository;
import com.noura.pricing.service.ProductPricingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Legacy pricing endpoints retained for admin-web compatibility.
 *
 * <p>The current extracted pricing core is product-centric. This controller keeps the old
 * price-list and variant-oriented admin contracts alive by storing lightweight list metadata
 * and translating variant IDs to product IDs before delegating to {@link ProductPricingService}.</p>
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("${app.api.version-prefix:/api/v1}")
public class PricingLegacyCompatibilityController {

    private final ProductPricingService productPricingService;
    private final LegacyPriceListRepository legacyPriceListRepository;
    private final CatalogVariantLookupClient catalogVariantLookupClient;

    /**
     * Lists admin-facing legacy price lists.
     *
     * @param request current request
     * @return price-list response envelope
     */
    @GetMapping("/price-lists")
    public ApiResponse<List<LegacyPriceListResponse>> listPriceLists(HttpServletRequest request) {
        List<LegacyPriceListResponse> data = legacyPriceListRepository.findAllByOrderByNameAsc().stream()
                .map(this::toPriceListResponse)
                .toList();
        return ApiResponse.ok("Price lists", data, request.getRequestURI());
    }

    /**
     * Creates one legacy price list for admin workflows.
     *
     * @param payload create payload
     * @param actorUserId optional actor identity
     * @param request current request
     * @return created price-list response envelope
     */
    @PostMapping("/price-lists")
    public ResponseEntity<ApiResponse<LegacyPriceListResponse>> createPriceList(
            @Valid @RequestBody LegacyPriceListRequest payload,
            @RequestHeader(value = "X-Auth-Subject", required = false) String actorUserId,
            HttpServletRequest request
    ) {
        LegacyPriceList entity = new LegacyPriceList();
        entity.setName(payload.name().trim());
        entity.setType(payload.type().trim().toUpperCase(Locale.ROOT));
        entity.setCustomerGroupId(payload.customerGroupId());
        entity.setChannelId(payload.channelId());
        entity.setCreatedBy(normalizeActor(actorUserId));
        entity.setUpdatedBy(normalizeActor(actorUserId));

        LegacyPriceList saved = legacyPriceListRepository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Price list created", toPriceListResponse(saved), request.getRequestURI()));
    }

    /**
     * Upserts one legacy variant price by translating the variant to its owning product.
     *
     * @param payload legacy upsert payload
     * @param actorUserId optional actor identity
     * @param request current request
     * @return saved legacy response envelope
     */
    @PostMapping("/prices")
    public ResponseEntity<ApiResponse<LegacyVariantPriceResponse>> upsertLegacyVariantPrice(
            @Valid @RequestBody LegacyVariantPriceUpsertRequest payload,
            @RequestHeader(value = "X-Auth-Subject", required = false) String actorUserId,
            HttpServletRequest request
    ) {
        LegacyPriceList priceList = requirePriceList(payload.priceListId());
        UUID productId = resolveProductId(payload.variantId());
        ProductPriceResponse saved = productPricingService.upsertPrice(
                new PriceUpsertRequest(
                        productId,
                        payload.currency().trim().toUpperCase(Locale.ROOT),
                        payload.amount(),
                        null,
                        resolveChannelCode(priceList),
                        null,
                        payload.startDate(),
                        payload.endDate(),
                        payload.priority(),
                        true
                ),
                normalizeActor(actorUserId)
        );

        LegacyVariantPriceResponse data = new LegacyVariantPriceResponse(
                saved.id(),
                payload.variantId(),
                priceList.getId(),
                saved.basePrice(),
                saved.currencyCode()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Price upserted", data, request.getRequestURI()));
    }

    /**
     * Quotes one legacy variant price by translating the variant to its owning product.
     *
     * @param variantId variant identifier
     * @param customerGroupId optional customer-group scope, retained for contract compatibility
     * @param channelId optional channel scope
     * @param request current request
     * @return quote response envelope
     */
    @GetMapping("/prices/variants/{variantId}")
    public ApiResponse<LegacyVariantPriceQuoteResponse> quoteLegacyVariantPrice(
            @PathVariable UUID variantId,
            @RequestParam(required = false) UUID customerGroupId,
            @RequestParam(required = false) UUID channelId,
            HttpServletRequest request
    ) {
        UUID productId = resolveProductId(variantId);
        PriceResolutionResponse resolved = productPricingService.resolveProductPrice(
                productId,
                null,
                null,
                channelId == null ? null : channelId.toString(),
                null
        );
        LegacyVariantPriceQuoteResponse data = new LegacyVariantPriceQuoteResponse(
                variantId,
                resolved.currencyCode(),
                resolved.basePrice(),
                resolved.effectivePrice(),
                List.of()
        );
        return ApiResponse.ok("Price quote", data, request.getRequestURI());
    }

    private LegacyPriceList requirePriceList(UUID priceListId) {
        return legacyPriceListRepository.findById(priceListId)
                .orElseThrow(() -> new NotFoundException("PRICE_LIST_NOT_FOUND", "Price list not found"));
    }

    private LegacyPriceListResponse toPriceListResponse(LegacyPriceList entity) {
        return new LegacyPriceListResponse(
                entity.getId(),
                entity.getName(),
                entity.getType(),
                entity.getCustomerGroupId(),
                entity.getChannelId()
        );
    }

    private UUID resolveProductId(UUID variantId) {
        try {
            return catalogVariantLookupClient.resolveProductId(variantId);
        } catch (NotFoundException ex) {
            // Some lower environments still key prices directly by product ID in the variant field.
            return variantId;
        }
    }

    private String resolveChannelCode(LegacyPriceList priceList) {
        return priceList.getChannelId() == null ? null : priceList.getChannelId().toString();
    }

    private String normalizeActor(String actorUserId) {
        return actorUserId == null || actorUserId.isBlank() ? "system" : actorUserId.trim();
    }
}
