package com.noura.platform.inventory.dto.warehouse;

public record WarehouseBinFilter(
        String warehouseId,
        String query,
        String zoneCode,
        Boolean active
) {
}
