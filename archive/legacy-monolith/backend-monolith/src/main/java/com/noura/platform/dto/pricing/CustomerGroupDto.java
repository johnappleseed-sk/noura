package com.noura.platform.dto.pricing;

public record CustomerGroupDto(
        Long id,
        String code,
        String name,
        Integer priority,
        Boolean active
) {
}
