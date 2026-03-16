package com.noura.platform.dto.inventory;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record InventoryTransferRequest(
        @NotNull UUID variantId,
        @NotNull UUID fromWarehouseId,
        @NotNull UUID toWarehouseId,
        @Min(1) int quantity,
        @FutureOrPresent Instant scheduledFor,
        String note
) {
}
