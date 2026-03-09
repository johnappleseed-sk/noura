package com.noura.platform.inventory.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record ProductRequest(
        @NotBlank @Size(max = 100) String sku,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 5000) String description,
        @NotBlank @Size(max = 40) String status,
        @NotNull @PositiveOrZero BigDecimal basePrice,
        @NotBlank @Size(min = 3, max = 3) String currencyCode,
        @PositiveOrZero BigDecimal widthCm,
        @PositiveOrZero BigDecimal heightCm,
        @PositiveOrZero BigDecimal lengthCm,
        @PositiveOrZero BigDecimal weightKg,
        Boolean batchTracked,
        Boolean serialTracked,
        @Size(max = 255) String barcodeValue,
        @Size(max = 255) String qrCodeValue,
        Boolean active,
        @NotEmpty List<@NotBlank @Size(max = 36) String> categoryIds,
        @Size(max = 36) String primaryCategoryId
) {
}
