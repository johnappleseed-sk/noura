package com.noura.inventory.dto.stock;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Stock adjustment command for a single product/location.
 *
 * @param productId product identifier
 * @param locationId warehouse/location identifier
 * @param quantityDelta signed quantity delta
 * @param lowStockThreshold optional low-stock threshold update
 * @param reasonCode optional reason code for governance/audit
 * @param referenceType optional external reference type
 * @param referenceId optional external reference identifier
 * @param notes optional operator note
 */
public record StockAdjustmentRequest(
        @NotNull UUID productId,
        @NotNull UUID locationId,
        @NotNull @DecimalMin(value = "-999999999", inclusive = true) BigDecimal quantityDelta,
        BigDecimal lowStockThreshold,
        String reasonCode,
        String referenceType,
        String referenceId,
        String notes
) {
}
