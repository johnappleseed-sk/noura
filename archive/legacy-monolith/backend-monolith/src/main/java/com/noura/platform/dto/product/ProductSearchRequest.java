package com.noura.platform.dto.product;

import com.noura.platform.domain.enums.ProductStatus;

import java.util.UUID;

public record ProductSearchRequest(
        String keyword,
        UUID categoryId,
        UUID brandId,
        ProductStatus status,
        int page,
        int size,
        String sortBy,
        String direction
) {
}
