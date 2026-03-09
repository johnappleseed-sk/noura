package com.noura.platform.inventory.dto.warehouse;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WarehouseBinRequest(
        @NotBlank @Size(max = 100) String binCode,
        @Size(max = 80) String zoneCode,
        @Size(max = 80) String aisleCode,
        @Size(max = 80) String shelfCode,
        @NotBlank @Size(max = 60) String binType,
        @Size(max = 255) String barcodeValue,
        @Size(max = 255) String qrCodeValue,
        @Min(0) Integer pickSequence,
        Boolean active
) {
}
