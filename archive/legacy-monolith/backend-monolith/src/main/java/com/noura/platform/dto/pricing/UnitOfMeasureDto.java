package com.noura.platform.dto.pricing;

public record UnitOfMeasureDto(
        Long id,
        String code,
        String name,
        Integer precisionScale,
        Boolean active
) {
}
