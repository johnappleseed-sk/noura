package com.noura.platform.dto.superinventory;

import com.noura.platform.domain.enums.SubmissionStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProductSubmissionDetailResponse(
        UUID id,
        UUID merchantId,
        UUID storeId,
        String proposedName,
        String proposedBrand,
        String proposedCategoryCode,
        Map<String, Object> proposedAttributesJson,
        String proposedBarcode,
        String proposedSku,
        String similarityHash,
        SubmissionStatus status,
        Instant submittedAt,
        Instant reviewedAt,
        String reviewedBy,
        String reviewNotes,
        UUID targetProductId,
        List<ProductApprovalDecisionResponse> decisions,
        Instant createdAt,
        Instant updatedAt
) {
}
