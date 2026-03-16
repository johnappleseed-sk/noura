package com.noura.platform.dto.inventory;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record WarehouseRequest(
        @NotBlank String name,
        @NotBlank String location,
        UUID storeId,
        Boolean active
) {
}
