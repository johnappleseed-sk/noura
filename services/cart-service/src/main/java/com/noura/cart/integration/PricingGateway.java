package com.noura.cart.integration;

import com.noura.cart.integration.model.PricingSnapshot;

import java.util.Optional;
import java.util.UUID;

/**
 * Pricing integration port used by cart validation and subtotaling.
 */
public interface PricingGateway {

    /**
     * Resolves effective unit price by product and optional store scope.
     *
     * @param productId product identifier
     * @param storeId optional store/location scope
     * @return pricing snapshot when available
     */
    Optional<PricingSnapshot> resolvePrice(UUID productId, UUID storeId);
}
