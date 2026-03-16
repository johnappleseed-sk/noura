package com.noura.platform.dto.inventory;

import java.util.List;
import java.util.UUID;

public record InventorySummaryDto(
        UUID variantId,
        List<InventoryLevelDto> levels
) {
}
