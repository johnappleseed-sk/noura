package com.noura.platform.dto.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Store-level adoption + override payload for a master product.
 */
public record StoreProductAdoptionRequest(
        @NotNull BigDecimal storePrice,
        @Min(0) Integer stock,
        Boolean published,
        Boolean visible,
        String localName,
        String localDescription,
        String taxCode
) {
}

