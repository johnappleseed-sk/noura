package com.noura.platform.dto.contract;

import com.noura.platform.domain.enums.MerchantContractActionType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record MerchantContractActionDto(
        UUID id,
        UUID contractId,
        MerchantContractActionType action,
        String actorEmail,
        String note,
        String correlationId,
        Map<String, Object> metadata,
        Instant occurredAt
) {
}

