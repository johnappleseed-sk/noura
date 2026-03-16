package com.noura.cart.dto.cart;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/**
 * Command payload for adding an item to the current cart.
 *
 * @param productId target product ID
 * @param variantId optional product variant ID
 * @param storeId optional store/location scope
 * @param quantity quantity delta to add (must be positive)
 * @param analyticsListName optional analytics metadata from storefront UI
 * @param analyticsSlot optional analytics slot index
 * @param analyticsPagePath optional analytics page path
 */
public record AddCartItemRequest(
        @NotNull UUID productId,
        UUID variantId,
        UUID storeId,
        @NotNull @Positive Integer quantity,
        String analyticsListName,
        Integer analyticsSlot,
        String analyticsPagePath
) {
}
