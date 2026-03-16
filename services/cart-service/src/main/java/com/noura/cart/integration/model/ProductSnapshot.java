package com.noura.cart.integration.model;

import java.util.UUID;

/**
 * Normalized product snapshot fetched from catalog service.
 *
 * @param productId product identifier
 * @param productName product display name
 * @param productCode product code (nullable when unavailable)
 * @param sku representative SKU (nullable)
 * @param allowBackorder whether checkout is allowed without available stock
 */
public record ProductSnapshot(
        UUID productId,
        String productName,
        String productCode,
        String sku,
        boolean allowBackorder
) {
}
