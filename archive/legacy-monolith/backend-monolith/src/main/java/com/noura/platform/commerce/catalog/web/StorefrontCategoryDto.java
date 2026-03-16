package com.noura.platform.commerce.catalog.web;

public record StorefrontCategoryDto(
        Long id,
        String name,
        String description,
        String imageUrl,
        Integer sortOrder,
        long productCount
) {
}
