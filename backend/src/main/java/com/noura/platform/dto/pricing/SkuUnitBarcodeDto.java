package com.noura.platform.dto.pricing;

public record SkuUnitBarcodeDto(
        Long id,
        Long sellUnitId,
        String barcode,
        Boolean isPrimary,
        Boolean active
) {
}
