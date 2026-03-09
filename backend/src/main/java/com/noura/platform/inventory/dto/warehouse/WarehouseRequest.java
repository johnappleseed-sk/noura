package com.noura.platform.inventory.dto.warehouse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WarehouseRequest(
        @NotBlank @Size(max = 80) String warehouseCode,
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 60) String warehouseType,
        @Size(max = 255) String addressLine1,
        @Size(max = 255) String addressLine2,
        @Size(max = 120) String city,
        @Size(max = 120) String stateProvince,
        @Size(max = 40) String postalCode,
        @Size(max = 2) String countryCode,
        Boolean active
) {
}
