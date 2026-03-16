package com.noura.inventory.dto.stock;

import com.noura.inventory.domain.enums.StockStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Read model for stock visibility endpoints.
 *
 * @param id stock-level record identifier
 * @param productId product identifier
 * @param productSku product SKU display value
 * @param productName product display name
 * @param warehouseId warehouse/location identifier
 * @param warehouseCode warehouse/location code
 * @param warehouseName warehouse/location name
 * @param binId optional bin identifier
 * @param binCode optional bin code
 * @param batchId optional batch identifier
 * @param lotNumber optional lot number
 * @param quantityOnHand on-hand quantity
 * @param quantityReserved reserved quantity
 * @param quantityAvailable available quantity
 * @param quantityDamaged damaged quantity
 * @param lastMovementAt timestamp of last stock movement
 * @param lowStock derived low-stock flag
 * @param lowStockThreshold configured threshold for low-stock state
 * @param stockStatus derived stock status
 * @param updatedAt record update timestamp
 */
public record StockLevelResponse(
        UUID id,
        UUID productId,
        String productSku,
        String productName,
        UUID warehouseId,
        String warehouseCode,
        String warehouseName,
        UUID binId,
        String binCode,
        UUID batchId,
        String lotNumber,
        BigDecimal quantityOnHand,
        BigDecimal quantityReserved,
        BigDecimal quantityAvailable,
        BigDecimal quantityDamaged,
        Instant lastMovementAt,
        boolean lowStock,
        BigDecimal lowStockThreshold,
        StockStatus stockStatus,
        Instant updatedAt
) {
}
