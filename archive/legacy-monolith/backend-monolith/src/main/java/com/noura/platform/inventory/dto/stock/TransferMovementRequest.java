package com.noura.platform.inventory.dto.stock;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TransferMovementRequest(
        @NotBlank @Size(max = 36) String sourceWarehouseId,
        @Size(max = 36) String sourceBinId,
        @NotBlank @Size(max = 36) String destinationWarehouseId,
        @Size(max = 36) String destinationBinId,
        @Size(max = 60) String referenceType,
        @Size(max = 120) String referenceId,
        @Size(max = 120) String externalReference,
        @Size(max = 1000) String notes,
        @NotEmpty List<@Valid StockMovementLineRequest> lines
) {
}
