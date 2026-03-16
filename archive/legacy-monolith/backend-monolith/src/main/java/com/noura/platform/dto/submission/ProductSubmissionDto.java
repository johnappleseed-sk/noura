package com.noura.platform.dto.submission;

import com.noura.platform.domain.enums.ProductSubmissionStatus;

import java.time.Instant;
import java.util.UUID;

public record ProductSubmissionDto(
        UUID id,
        UUID storeId,
        String storeName,
        UUID merchantId,
        String merchantName,
        int revisionNumber,
        ProductSubmissionStatus status,
        boolean potentialDuplicate,
        UUID matchedMasterProductId,
        String requestedByEmail,
        Instant createdAt,
        Instant reviewedAt
) {
}

