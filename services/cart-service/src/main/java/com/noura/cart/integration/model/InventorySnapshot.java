package com.noura.cart.integration.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Normalized inventory availability snapshot fetched from inventory service.
 *
 * @param productId product identifier
 * @param storeId requested store/location scope
 * @param availableQuantity aggregated available quantity
 * @param hasInventoryRows true when inventory service returned at least one stock row
 */
public record InventorySnapshot(
        UUID productId,
        UUID storeId,
        BigDecimal availableQuantity,
        boolean hasInventoryRows
) {
}
