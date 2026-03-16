package com.noura.inventory.dto.stock;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Stock deduction command.
 *
 * @param productId product identifier
 * @param locationId warehouse/location identifier
 * @param quantity quantity to deduct
 * @param consumeReserved when true, deduction consumes reserved stock first
 * @param reasonCode optional reason code for governance/audit
 * @param referenceType optional external reference type
 * @param referenceId optional external reference identifier
 * @param notes optional operator note
 */
public record StockDeductionRequest(
        @NotNull UUID productId,
        @NotNull UUID locationId,
        @NotNull @Positive BigDecimal quantity,
        Boolean consumeReserved,
        String reasonCode,
        String referenceType,
        String referenceId,
        String notes
) {
}
