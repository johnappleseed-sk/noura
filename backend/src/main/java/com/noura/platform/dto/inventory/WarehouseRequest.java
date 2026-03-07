package com.noura.platform.dto.inventory;

import jakarta.validation.constraints.NotBlank;

public record WarehouseRequest(
        @NotBlank String name,
        @NotBlank String location,
        Boolean active
) {
}
