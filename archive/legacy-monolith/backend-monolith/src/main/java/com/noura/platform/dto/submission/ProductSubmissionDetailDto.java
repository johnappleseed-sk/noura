package com.noura.platform.dto.submission;

import com.noura.platform.domain.enums.ProductSubmissionStatus;
import com.noura.platform.dto.product.ProductRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductSubmissionDetailDto(
        UUID id,
        UUID storeId,
        String storeName,
        UUID merchantId,
        String merchantName,
        UUID parentSubmissionId,
        int revisionNumber,
        ProductSubmissionStatus status,
        ProductRequest product,
        String submitNote,
        boolean potentialDuplicate,
        UUID matchedMasterProductId,
        String requestedByEmail,
        String reviewedByEmail,
        Instant reviewedAt,
        String reviewNote,
        List<ProductDedupeCandidateDto> dedupeCandidates,
        List<ProductSubmissionReviewDto> reviews,
        Instant createdAt
) {
}

