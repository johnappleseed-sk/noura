package com.noura.platform.dto.inventory;

import java.util.List;

public record InventoryCheckResultDto(
        List<InventoryCheckResultItemDto> items
) {
}
