package com.noura.platform.inventory.dto.stock;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record StockMovementLineRequest(
        @NotBlank @Size(max = 36) String productId,
        @Positive BigDecimal quantity,
        @Size(max = 36) String fromBinId,
        @Size(max = 36) String toBinId,
        @Size(max = 36) String batchId,
        @Size(max = 120) String lotNumber,
        LocalDate expiryDate,
        LocalDate manufacturedAt,
        @Size(max = 120) String supplierBatchRef,
        @PositiveOrZero BigDecimal unitCost,
        @Size(max = 500) String notes,
        List<@NotBlank @Size(max = 180) String> serialNumbers
) {
}
