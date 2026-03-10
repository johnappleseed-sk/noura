package com.noura.platform.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AddCartItemRequest(
        @NotNull UUID productId,
        UUID variantId,
        @Min(1) int quantity,
        UUID storeId,
        @Size(max = 80) String analyticsListName,
        Integer analyticsSlot,
        @Size(max = 255) String analyticsPagePath
) {
}
