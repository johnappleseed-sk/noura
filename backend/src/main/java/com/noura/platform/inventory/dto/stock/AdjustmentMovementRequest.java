package com.noura.platform.inventory.dto.stock;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AdjustmentMovementRequest(
        @NotBlank @Size(max = 36) String warehouseId,
        @Size(max = 36) String binId,
        @Size(max = 80) String reasonCode,
        @Size(max = 120) String referenceId,
        @Size(max = 120) String externalReference,
        @Size(max = 1000) String notes,
        @NotEmpty List<@Valid AdjustmentMovementLineRequest> lines
) {
}
