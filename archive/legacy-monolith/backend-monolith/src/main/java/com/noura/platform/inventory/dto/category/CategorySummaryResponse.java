package com.noura.platform.inventory.dto.category;

public record CategorySummaryResponse(
        String id,
        String categoryCode,
        String name,
        int level,
        boolean active
) {
}
