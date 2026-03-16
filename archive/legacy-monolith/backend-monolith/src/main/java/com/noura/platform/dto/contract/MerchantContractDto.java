package com.noura.platform.dto.contract;

import com.noura.platform.domain.enums.MerchantContractStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record MerchantContractDto(
        UUID id,
        UUID merchantId,
        String merchantName,
        String contractNumber,
        MerchantContractStatus status,
        LocalDate startDate,
        LocalDate endDate,
        String requestedByEmail,
        String reviewedByEmail,
        Instant reviewedAt,
        String reviewNote,
        Map<String, Object> terms,
        Instant createdAt
) {
}

