package com.noura.platform.domain.enums;

/**
 * Enumerates lifecycle states used by the destructive-action governance layer.
 */
public enum RecoveryLifecycleState {
    ACTIVE,
    INACTIVE,
    ARCHIVED,
    TRASHED,
    PURGED,
    ANONYMIZED
}
