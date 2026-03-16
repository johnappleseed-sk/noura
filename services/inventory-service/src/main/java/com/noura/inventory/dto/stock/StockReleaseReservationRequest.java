package com.noura.inventory.dto.stock;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Command to release previously reserved stock.
 *
 * @param productId product identifier
 * @param locationId warehouse/location identifier
 * @param quantity quantity to release
 * @param reasonCode optional reason code for governance/audit
 * @param referenceType optional external reference type
 * @param referenceId optional external reference identifier
 * @param notes optional operator note
 */
public record StockReleaseReservationRequest(
        @NotNull UUID productId,
        @NotNull UUID locationId,
        @NotNull @Positive BigDecimal quantity,
        String reasonCode,
        String referenceType,
        String referenceId,
        String notes
) {
}
