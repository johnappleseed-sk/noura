package com.noura.platform.dto.inventory;

import java.util.UUID;

public record WarehouseDto(
        UUID id,
        String name,
        String location,
        boolean active
) {
}
