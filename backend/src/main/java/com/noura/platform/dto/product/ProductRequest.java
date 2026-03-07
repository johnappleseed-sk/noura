package com.noura.platform.dto.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProductRequest(
        @NotBlank String name,
        String description,
        UUID categoryId,
        String category,
        String brand,
        BigDecimal price,
        Map<String, Object> attributes,
        boolean allowBackorder,
        boolean flashSale,
        boolean trending,
        boolean bestSeller,
        String shortDescription,
        String longDescription,
        @Valid ProductSeoRequest seo,
        String seoTitle,
        String seoDescription,
        String seoSlug,
        List<@Valid ProductVariantRequest> variants,
        List<@Valid ProductMediaRequest> media,
        List<@Valid ProductInventoryRequest> inventory
) {
}
