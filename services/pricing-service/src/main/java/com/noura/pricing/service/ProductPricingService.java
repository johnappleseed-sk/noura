package com.noura.pricing.service;

import com.noura.pricing.dto.price.BulkPriceResolutionResponse;
import com.noura.pricing.dto.price.PriceResolutionResponse;
import com.noura.pricing.dto.price.PriceUpsertRequest;
import com.noura.pricing.dto.price.ProductPriceResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Application service for price upsert and price resolution use cases.
 */
public interface ProductPricingService {

    /**
     * Resolves a price for a single product.
     *
     * @param productId product identifier
     * @param currencyCode optional currency code
     * @param storeId optional store scope
     * @param channelCode optional channel scope
     * @param at optional resolution timestamp
     * @return resolved price
     */
    PriceResolutionResponse resolveProductPrice(
            UUID productId,
            String currencyCode,
            UUID storeId,
            String channelCode,
            Instant at
    );

    /**
     * Resolves prices for multiple products.
     *
     * @param productIds product identifiers
     * @param currencyCode optional currency code
     * @param storeId optional store scope
     * @param channelCode optional channel scope
     * @param at optional resolution timestamp
     * @return bulk resolution response with missing product IDs
     */
    BulkPriceResolutionResponse resolveBulkPrices(
            List<UUID> productIds,
            String currencyCode,
            UUID storeId,
            String channelCode,
            Instant at
    );

    /**
     * Resolves active storefront snapshot for requested products.
     *
     * @param productIds product identifiers
     * @param currencyCode optional currency code
     * @param storeId optional store scope
     * @param channelCode optional channel scope
     * @param at optional resolution timestamp
     * @return active snapshot list
     */
    List<PriceResolutionResponse> activeSnapshot(
            List<UUID> productIds,
            String currencyCode,
            UUID storeId,
            String channelCode,
            Instant at
    );

    /**
     * Upserts a product price row using natural-key semantics.
     *
     * @param request upsert command
     * @param actorUserId optional actor identity for audit fields
     * @return persisted product price
     */
    ProductPriceResponse upsertPrice(PriceUpsertRequest request, String actorUserId);
}

