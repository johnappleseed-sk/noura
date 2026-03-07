package com.noura.platform.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddCartItemRequest(
        @NotNull UUID productId,
        UUID variantId,
        @Min(1) int quantity,
        UUID storeId
) {
}
