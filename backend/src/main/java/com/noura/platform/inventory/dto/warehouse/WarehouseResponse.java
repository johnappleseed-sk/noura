package com.noura.platform.inventory.dto.warehouse;

import java.time.Instant;

public record WarehouseResponse(
        String id,
        String warehouseCode,
        String name,
        String warehouseType,
        String addressLine1,
        String addressLine2,
        String city,
        String stateProvince,
        String postalCode,
        String countryCode,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
