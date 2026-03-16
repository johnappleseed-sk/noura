package com.noura.platform.dto.superinventory;

import com.noura.platform.domain.enums.ApprovalDecisionType;

import java.time.Instant;
import java.util.UUID;

public record ProductApprovalDecisionResponse(
        UUID id,
        UUID submissionId,
        ApprovalDecisionType decisionType,
        UUID targetProductId,
        String notes,
        Instant decidedAt,
        String decidedBy
) {
}
