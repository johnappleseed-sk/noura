package com.noura.platform.dto.catalog;

import com.noura.platform.domain.enums.CategoryChangeAction;
import com.noura.platform.domain.enums.CategoryChangeRequestStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record CategoryChangeRequestDto(
        UUID id,
        UUID categoryId,
        CategoryChangeAction action,
        CategoryChangeRequestStatus status,
        UUID requestedByUserId,
        UUID reviewedByUserId,
        Map<String, Object> payload,
        String reason,
        String reviewComment,
        Instant createdAt,
        Instant reviewedAt
) {
}
