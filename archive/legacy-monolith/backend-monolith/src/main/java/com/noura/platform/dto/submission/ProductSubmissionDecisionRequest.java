package com.noura.platform.dto.submission;

import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ProductSubmissionDecisionRequest(
        @Size(max = 1000) String note,
        UUID existingMasterProductId
) {
}
