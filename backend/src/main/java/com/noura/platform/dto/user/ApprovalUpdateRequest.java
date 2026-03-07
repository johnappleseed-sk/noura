package com.noura.platform.dto.user;

import com.noura.platform.domain.enums.ApprovalStatus;
import jakarta.validation.constraints.NotNull;

public record ApprovalUpdateRequest(
        @NotNull ApprovalStatus status,
        String reviewerNotes
) {
}
