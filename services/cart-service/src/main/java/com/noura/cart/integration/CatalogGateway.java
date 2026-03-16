package com.noura.cart.integration;

import com.noura.cart.integration.model.ProductSnapshot;

import java.util.Optional;
import java.util.UUID;

/**
 * Catalog integration port used by cart validation flows.
 */
public interface CatalogGateway {

    /**
     * Fetches product snapshot by product ID.
     *
     * @param productId product identifier
     * @return product snapshot when available
     */
    Optional<ProductSnapshot> findProduct(UUID productId);
}
