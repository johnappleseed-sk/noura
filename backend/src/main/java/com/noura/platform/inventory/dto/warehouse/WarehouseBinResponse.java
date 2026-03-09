package com.noura.platform.inventory.dto.warehouse;

import java.time.Instant;

public record WarehouseBinResponse(
        String id,
        WarehouseSummaryResponse warehouse,
        String binCode,
        String zoneCode,
        String aisleCode,
        String shelfCode,
        String binType,
        String barcodeValue,
        String qrCodeValue,
        int pickSequence,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
