package com.noura.platform.dto.inventory;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record InventoryRestockScheduleRequest(
        @NotNull UUID variantId,
        @NotNull UUID warehouseId,
        @Min(1) int targetQuantity,
        @NotNull @FutureOrPresent Instant scheduledFor,
        String note
) {
}
