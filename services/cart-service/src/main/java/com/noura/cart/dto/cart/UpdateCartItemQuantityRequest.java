package com.noura.cart.dto.cart;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Command payload for replacing a cart line quantity.
 *
 * @param quantity replacement quantity (must be positive)
 */
public record UpdateCartItemQuantityRequest(
        @NotNull @Positive Integer quantity
) {
}
