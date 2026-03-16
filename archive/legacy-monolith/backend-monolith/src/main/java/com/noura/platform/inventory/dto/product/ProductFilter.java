package com.noura.platform.inventory.dto.product;

public record ProductFilter(
        String query,
        String categoryId,
        Boolean active
) {
}
