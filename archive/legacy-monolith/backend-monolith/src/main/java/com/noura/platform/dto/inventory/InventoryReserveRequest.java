package com.noura.platform.dto.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InventoryReserveRequest(
        @NotNull UUID variantId,
        @NotNull UUID warehouseId,
        UUID orderId,
        @Min(1) int quantity,
        String note
) {
}
