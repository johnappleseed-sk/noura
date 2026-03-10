package com.noura.platform.dto.inventory;

import java.util.UUID;

public record LowStockAlertDto(
        UUID inventoryId,
        UUID variantId,
        UUID warehouseId,
        String warehouseName,
        int quantity,
        int reservedQuantity,
        int availableQuantity,
        int reorderPoint
) {
}
