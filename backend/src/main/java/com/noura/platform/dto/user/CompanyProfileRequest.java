package com.noura.platform.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CompanyProfileRequest(
        @NotBlank String companyName,
        String taxId,
        String costCenter,
        String approvalEmail,
        boolean approvalRequired,
        @NotNull BigDecimal approvalThreshold
) {
}
