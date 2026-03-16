package com.noura.platform.dto.recovery;

import com.noura.platform.domain.enums.RecoveryLifecycleState;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a governed recovery record returned to the admin recovery center.
 *
 * @param id The recovery record identifier.
 * @param tenantKey The tenant scope.
 * @param entityType The business entity type.
 * @param entityId The business entity identifier.
 * @param displayName The operator-facing display name.
 * @param lifecycleState The governed lifecycle state.
 * @param currentVersionNumber The current snapshot version number.
 * @param backupVerified Whether a hard-delete backup has been verified.
 * @param anonymized Whether recovery snapshots have been anonymized.
 * @param retentionUntil The retention expiry timestamp.
 * @param legalHoldUntil The legal-hold expiry timestamp.
 * @param deletedAt The trash timestamp.
 * @param archivedAt The archive timestamp.
 * @param inactivatedAt The inactive timestamp.
 * @param restoredAt The restore timestamp.
 * @param purgedAt The purge timestamp.
 * @param lastActionBy The last operator recorded on the record.
 * @param lastReason The last reason recorded on the record.
 * @param updatedAt The last update timestamp.
 */
public record RecoveryRecordDto(
        UUID id,
        String tenantKey,
        String entityType,
        String entityId,
        String displayName,
        RecoveryLifecycleState lifecycleState,
        Integer currentVersionNumber,
        boolean backupVerified,
        boolean anonymized,
        Instant retentionUntil,
        Instant legalHoldUntil,
        Instant deletedAt,
        Instant archivedAt,
        Instant inactivatedAt,
        Instant restoredAt,
        Instant purgedAt,
        String lastActionBy,
        String lastReason,
        Instant updatedAt
) {
}
