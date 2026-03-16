package com.noura.platform.dto.submission;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record ProductDedupeCandidateDto(
        UUID id,
        UUID masterProductId,
        String masterProductName,
        String barcode,
        BigDecimal matchScore,
        String matchReason,
        Map<String, Object> detail
) {
}

