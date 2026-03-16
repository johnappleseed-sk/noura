package com.noura.platform.dto.user;

import com.noura.platform.domain.enums.ApprovalStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record ApprovalDto(
        UUID id,
        UUID requesterId,
        UUID orderId,
        BigDecimal amount,
        ApprovalStatus status,
        String reviewerNotes
) {
}
