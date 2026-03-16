package com.noura.platform.dto.contract;

import jakarta.validation.constraints.NotBlank;

public record MerchantCreateRequest(
        @NotBlank String name,
        String legalName,
        String taxId,
        String primaryEmail,
        String primaryPhone,
        String notes
) {
}

