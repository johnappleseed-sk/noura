package com.noura.platform.dto.product;

import com.noura.platform.domain.enums.ProductStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProductDto(
        UUID id,
        String name,
        String category,
        String brand,
        BigDecimal price,
        boolean flashSale,
        boolean trending,
        boolean bestSeller,
        double averageRating,
        int reviewCount,
        int popularityScore,
        String shortDescription,
        String longDescription,
        String seoTitle,
        String seoDescription,
        String seoSlug,
        ProductSeoDto seo,
        Map<String, Object> attributes,
        ProductStatus status,
        boolean active,
        boolean allowBackorder,
        List<ProductVariantDto> variants,
        List<ProductMediaDto> media,
        List<ProductStoreInventoryDto> storeInventory
) {
}
