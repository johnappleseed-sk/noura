package com.noura.platform.inventory.dto.serial;

public record SerialNumberFilter(
        String query,
        String productId,
        String serialStatus,
        String warehouseId,
        String binId,
        String batchId
) {
}
