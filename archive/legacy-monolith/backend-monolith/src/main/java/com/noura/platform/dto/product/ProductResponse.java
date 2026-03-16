package com.noura.platform.dto.product;

import com.noura.platform.domain.enums.ProductStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String productCode,
        String name,
        String slug,
        UUID brandId,
        UUID categoryId,
        String shortDescription,
        String longDescription,
        ProductStatus status,
        String approvalStatus,
        Instant createdAt,
        Instant updatedAt,
        List<ProductVariantResponse> variants
) {
}
