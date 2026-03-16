package com.noura.platform.dto.recovery;

import com.noura.platform.domain.enums.RecoveryActionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Defines a bulk governed destructive-action request.
 *
 * @param entityType The business entity type.
 * @param actionType The requested action.
 * @param entityIds The business entity identifiers.
 * @param reason The operator-facing reason for the action.
 * @param dryRun Whether the request should only validate and not mutate state.
 * @param restoreTo The target point-in-time restore timestamp.
 * @param legalHoldUntil The legal-hold expiry timestamp.
 * @param retentionDays The retention override, in days.
 * @param metadata Optional structured metadata recorded with the action.
 */
public record RecoveryBulkActionRequest(
        @NotBlank String entityType,
        @NotNull RecoveryActionType actionType,
        @NotEmpty List<@NotBlank String> entityIds,
        String reason,
        boolean dryRun,
        Instant restoreTo,
        Instant legalHoldUntil,
        @PositiveOrZero Integer retentionDays,
        Map<String, Object> metadata
) {
}
