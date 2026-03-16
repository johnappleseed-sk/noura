package com.noura.platform.dto.recovery;

import com.noura.platform.domain.enums.RecoveryActionType;
import com.noura.platform.domain.enums.RecoveryLifecycleState;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a single version snapshot returned to the admin recovery center.
 *
 * @param id The version identifier.
 * @param versionNumber The monotonically increasing version number.
 * @param actionType The action that created the version.
 * @param lifecycleStateAfter The lifecycle state after the captured action.
 * @param actor The operator that created the version.
 * @param reason The operator-facing reason for the version.
 * @param backupSnapshot Whether the version is a hard-delete backup.
 * @param anonymized Whether the snapshot payload is anonymized.
 * @param restoredFromAt The point-in-time restore source timestamp.
 * @param capturedAt The snapshot timestamp.
 * @param metadataJson The recorded metadata payload.
 * @param snapshotJson The serialized snapshot payload.
 */
public record RecoveryVersionDto(
        UUID id,
        Integer versionNumber,
        RecoveryActionType actionType,
        RecoveryLifecycleState lifecycleStateAfter,
        String actor,
        String reason,
        boolean backupSnapshot,
        boolean anonymized,
        Instant restoredFromAt,
        Instant capturedAt,
        String metadataJson,
        String snapshotJson
) {
}
