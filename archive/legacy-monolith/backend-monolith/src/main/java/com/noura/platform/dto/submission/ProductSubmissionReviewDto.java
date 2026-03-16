package com.noura.platform.dto.submission;

import com.noura.platform.domain.enums.ProductSubmissionReviewAction;

import java.time.Instant;
import java.util.UUID;

public record ProductSubmissionReviewDto(
        UUID id,
        ProductSubmissionReviewAction action,
        String reviewerEmail,
        String note,
        UUID masterProductId,
        Instant occurredAt
) {
}

