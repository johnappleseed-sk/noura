package com.noura.cart.integration;

import com.noura.cart.integration.model.InventorySnapshot;

import java.util.UUID;

/**
 * Inventory integration port used by cart availability validation.
 */
public interface InventoryGateway {

    /**
     * Resolves available quantity for a product and optional store scope.
     *
     * @param productId product identifier
     * @param storeId optional store/location scope
     * @return normalized availability snapshot
     */
    InventorySnapshot resolveAvailability(UUID productId, UUID storeId);
}
