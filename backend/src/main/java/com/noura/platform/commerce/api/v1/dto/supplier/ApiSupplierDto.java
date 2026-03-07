package com.noura.platform.commerce.api.v1.dto.supplier;

public record ApiSupplierDto(
        Long id,
        String name,
        String phone,
        String email,
        String address,
        String status
) {
}
