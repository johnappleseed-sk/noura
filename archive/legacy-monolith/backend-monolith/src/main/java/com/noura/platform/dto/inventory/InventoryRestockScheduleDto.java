package com.noura.platform.dto.inventory;

import com.noura.platform.domain.enums.InventoryRestockScheduleStatus;

import java.time.Instant;
import java.util.UUID;

public record InventoryRestockScheduleDto(
        UUID id,
        UUID variantId,
        UUID warehouseId,
        String warehouseName,
        int targetQuantity,
        InventoryRestockScheduleStatus status,
        Instant scheduledFor,
        String requestedBy,
        String note,
        Instant createdAt
) {
}
