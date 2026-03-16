package com.noura.inventory.dto.stock;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Line item for bulk adjustment compatibility endpoint.
 *
 * @param productId product to adjust
 * @param quantityDelta signed delta to apply (positive adds stock, negative removes stock)
 * @param binId optional bin identifier override for this line
 * @param batchId optional batch identifier
 * @param lotNumber optional lot number
 * @param notes optional operator note
 */
public record AdjustmentMovementLineRequest(
        @NotNull UUID productId,
        @NotNull BigDecimal quantityDelta,
        UUID binId,
        UUID batchId,
        String lotNumber,
        String notes
) {
}
