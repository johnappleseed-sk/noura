package com.noura.platform.inventory.dto.stock;

import java.time.Instant;

public record StockMovementFilter(
        String movementType,
        String movementStatus,
        String warehouseId,
        String productId,
        String referenceQuery,
        Instant processedFrom,
        Instant processedTo
) {
}
