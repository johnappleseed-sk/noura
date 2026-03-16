package com.noura.platform.inventory.dto.stock;

public record StockLevelFilter(
        String productId,
        String warehouseId,
        String binId,
        String batchId,
        Boolean lowStockOnly
) {
}
