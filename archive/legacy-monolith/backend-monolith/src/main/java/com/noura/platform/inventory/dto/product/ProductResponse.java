package com.noura.platform.inventory.dto.product;

import com.noura.platform.inventory.dto.category.CategorySummaryResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductResponse(
        String id,
        String sku,
        String name,
        String description,
        String status,
        BigDecimal basePrice,
        String currencyCode,
        BigDecimal widthCm,
        BigDecimal heightCm,
        BigDecimal lengthCm,
        BigDecimal weightKg,
        boolean batchTracked,
        boolean serialTracked,
        String barcodeValue,
        String qrCodeValue,
        boolean active,
        CategorySummaryResponse primaryCategory,
        List<CategorySummaryResponse> categories,
        Instant createdAt,
        Instant updatedAt
) {
}
