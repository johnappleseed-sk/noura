package com.noura.platform.dto.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductInventoryRequest(
        @NotNull UUID storeId,
        @Min(0) int stock,
        @NotNull BigDecimal storePrice
) {
}
