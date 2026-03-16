package com.noura.platform.inventory.dto.category;

public record CategoryFilter(
        String query,
        String parentId,
        Boolean active
) {
}
