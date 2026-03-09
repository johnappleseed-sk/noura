package com.noura.platform.inventory.dto.serial;

import java.time.Instant;

public record SerialNumberResponse(
        String id,
        String productId,
        String productSku,
        String productName,
        String batchId,
        String lotNumber,
        String warehouseId,
        String warehouseCode,
        String binId,
        String binCode,
        String serialNumber,
        String serialStatus,
        String lastMovementLineId,
        Instant updatedAt
) {
}
