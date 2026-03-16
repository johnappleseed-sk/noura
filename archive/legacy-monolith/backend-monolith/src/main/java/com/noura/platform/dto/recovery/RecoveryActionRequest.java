package com.noura.platform.dto.recovery;

import com.noura.platform.domain.enums.RecoveryActionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;
import java.util.Map;

/**
 * Defines a single governed destructive or recovery action request.
 *
 * @param entityType The business entity type.
 * @param entityId The business entity identifier.
 * @param actionType The requested action.
 * @param reason The operator-facing reason for the action.
 * @param restoreTo The target point-in-time restore timestamp.
 * @param legalHoldUntil The legal-hold expiry timestamp.
 * @param retentionDays The retention override, in days.
 * @param metadata Optional structured metadata recorded with the action.
 */
public record RecoveryActionRequest(
        @NotBlank String entityType,
        @NotBlank String entityId,
        @NotNull RecoveryActionType actionType,
        String reason,
        Instant restoreTo,
        Instant legalHoldUntil,
        @PositiveOrZero Integer retentionDays,
        Map<String, Object> metadata
) {
}
