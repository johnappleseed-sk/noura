package com.noura.platform.inventory.dto.stock;

import java.time.Instant;
import java.util.List;

public record StockMovementResponse(
        String id,
        String movementNumber,
        String movementType,
        String movementStatus,
        String sourceWarehouseId,
        String sourceWarehouseCode,
        String sourceBinId,
        String sourceBinCode,
        String destinationWarehouseId,
        String destinationWarehouseCode,
        String destinationBinId,
        String destinationBinCode,
        String referenceType,
        String referenceId,
        String externalReference,
        String notes,
        String processedBy,
        Instant processedAt,
        Instant createdAt,
        List<StockMovementLineResponse> lines
) {
}
