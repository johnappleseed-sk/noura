package com.noura.platform.dto.product;

public record ProductSeoRequest(
        String slug,
        String metaTitle,
        String metaDescription
) {
}
