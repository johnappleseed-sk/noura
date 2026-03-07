package com.noura.platform.commerce.catalog.web;

import java.math.BigDecimal;
import java.util.List;

public record StorefrontProductDetailDto(
        Long id,
        String sku,
        String barcode,
        String name,
        BigDecimal price,
        String imageUrl,
        Long categoryId,
        String categoryName,
        Integer stockQty,
        Integer lowStockThreshold,
        boolean active,
        boolean allowNegativeStock,
        String baseUnitName,
        Integer baseUnitPrecision,
        String boxSpecifications,
        BigDecimal weightValue,
        String weightUnit,
        BigDecimal lengthValue,
        String lengthUnit,
        BigDecimal widthValue,
        String widthUnit,
        BigDecimal heightValue,
        String heightUnit,
        List<StorefrontProductUnitDto> units
) {
}
