package com.noura.platform.dto.inventory;

import com.noura.platform.domain.enums.InventoryReservationStatus;

import java.time.Instant;
import java.util.UUID;

public record InventoryReservationDto(
        UUID id,
        UUID variantId,
        UUID warehouseId,
        UUID orderId,
        int quantity,
        InventoryReservationStatus status,
        String note,
        Instant createdAt
) {
}
