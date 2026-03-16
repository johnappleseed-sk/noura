package com.noura.platform.dto.inventory;

import java.util.UUID;

public record InventoryCheckResultItemDto(
        UUID variantId,
        UUID warehouseId,
        int requestedQuantity,
        int availableQuantity,
        boolean available,
        boolean backorder
) {
}
