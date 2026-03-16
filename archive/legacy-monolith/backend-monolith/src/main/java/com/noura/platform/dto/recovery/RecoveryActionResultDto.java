package com.noura.platform.dto.recovery;

import com.noura.platform.domain.enums.RecoveryActionType;
import com.noura.platform.domain.enums.RecoveryLifecycleState;

/**
 * Represents the result of a single governed destructive or recovery action.
 *
 * @param entityType The business entity type.
 * @param entityId The business entity identifier.
 * @param actionType The executed action.
 * @param lifecycleState The resulting lifecycle state.
 * @param message The operator-facing result message.
 * @param record The updated recovery record.
 */
public record RecoveryActionResultDto(
        String entityType,
        String entityId,
        RecoveryActionType actionType,
        RecoveryLifecycleState lifecycleState,
        String message,
        RecoveryRecordDto record
) {
}
