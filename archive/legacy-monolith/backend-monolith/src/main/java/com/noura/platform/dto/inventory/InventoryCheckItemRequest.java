package com.noura.platform.dto.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InventoryCheckItemRequest(
        @NotNull UUID variantId,
        @NotNull UUID warehouseId,
        @Min(1) int quantity
) {
}
