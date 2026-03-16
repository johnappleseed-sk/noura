package com.noura.platform.dto.product;

import com.noura.platform.domain.enums.ProductStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductSearchResponse(
        UUID id,
        String productCode,
        String name,
        String slug,
        UUID categoryId,
        String categoryName,
        UUID brandId,
        String brandName,
        ProductStatus status,
        String approvalStatus,
        boolean active,
        String barcode,
        List<String> skus,
        Instant createdAt,
        Instant updatedAt
) {
}
