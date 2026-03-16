package com.noura.inventory.dto.stock;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Bulk adjustment request used for legacy compatibility flow.
 *
 * @param warehouseId target warehouse/location for all lines
 * @param binId optional bin identifier applied to lines
 * @param reasonCode optional reason code for audit
 * @param referenceType optional external reference type (for example ORDER)
 * @param referenceId optional external reference identifier
 * @param notes optional request-level note
 * @param lines line adjustments to apply
 */
public record AdjustmentMovementRequest(
        @NotNull UUID warehouseId,
        UUID binId,
        String reasonCode,
        String referenceType,
        String referenceId,
        String notes,
        @NotEmpty List<@Valid AdjustmentMovementLineRequest> lines
) {
}
