package com.noura.platform.dto.inventory;

import com.noura.platform.domain.enums.InventoryTransferStatus;

import java.time.Instant;
import java.util.UUID;

public record InventoryTransferDto(
        UUID id,
        UUID variantId,
        UUID fromWarehouseId,
        String fromWarehouseName,
        UUID toWarehouseId,
        String toWarehouseName,
        int quantity,
        InventoryTransferStatus status,
        Instant scheduledFor,
        Instant completedAt,
        String requestedBy,
        String note,
        Instant createdAt
) {
}
