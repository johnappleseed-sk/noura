package com.noura.platform.commerce.catalog.web;

import java.math.BigDecimal;

public record StorefrontProductUnitDto(
        Long id,
        String name,
        String abbreviation,
        BigDecimal conversionToBase,
        boolean defaultSaleUnit
) {
}
