package com.noura.platform.dto.user;

import java.math.BigDecimal;
import java.util.UUID;

public record CompanyProfileDto(
        UUID id,
        String companyName,
        String taxId,
        String costCenter,
        String approvalEmail,
        boolean approvalRequired,
        BigDecimal approvalThreshold
) {
}
