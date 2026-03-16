package com.noura.platform.dto.inventory;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InventoryAdjustRequest(
        @NotNull UUID variantId,
        @NotNull UUID warehouseId,
        int changeQuantity,
        String reason,
        Integer reorderPoint
) {
}
