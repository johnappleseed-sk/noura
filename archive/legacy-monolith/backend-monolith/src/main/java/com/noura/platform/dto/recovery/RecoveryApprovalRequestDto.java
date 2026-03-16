package com.noura.platform.dto.recovery;

import com.noura.platform.domain.enums.RecoveryActionType;
import com.noura.platform.domain.enums.RecoveryApprovalKind;
import com.noura.platform.domain.enums.RecoveryApprovalStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a 4-eyes approval request returned to the admin recovery center.
 */
public record RecoveryApprovalRequestDto(
        UUID id,
        String tenantKey,
        RecoveryApprovalKind requestKind,
        String entityType,
        String entityId,
        RecoveryActionType actionType,
        RecoveryApprovalStatus status,
        Integer requestedItems,
        String reason,
        String changeTicket,
        String requestedBy,
        Instant requestedAt,
        String reviewedBy,
        Instant reviewedAt,
        String reviewerNotes,
        UUID executedJobId,
        Instant executedAt,
        String executionError,
        Instant updatedAt
){
}

