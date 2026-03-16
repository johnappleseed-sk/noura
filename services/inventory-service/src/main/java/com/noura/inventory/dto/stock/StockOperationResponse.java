package com.noura.inventory.dto.stock;

import com.noura.inventory.domain.enums.StockMovementType;

import java.util.UUID;

/**
 * Response returned by mutation operations.
 *
 * @param movementId created movement identifier
 * @param movementType operation type
 * @param stockLevel resulting stock state after mutation
 */
public record StockOperationResponse(
        UUID movementId,
        StockMovementType movementType,
        StockLevelResponse stockLevel
) {
}
