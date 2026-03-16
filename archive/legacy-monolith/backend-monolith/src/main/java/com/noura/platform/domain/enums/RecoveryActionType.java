package com.noura.platform.domain.enums;

/**
 * Enumerates destructive and recovery actions tracked by the recovery center.
 */
public enum RecoveryActionType {
    CREATE,
    UPDATE,
    ACTIVATE,
    DEACTIVATE,
    ARCHIVE,
    TRASH,
    RESTORE,
    UNDO_TRASH,
    HARD_DELETE,
    APPLY_LEGAL_HOLD,
    RELEASE_LEGAL_HOLD,
    ANONYMIZE,
    RESTORE_POINT_IN_TIME
}
