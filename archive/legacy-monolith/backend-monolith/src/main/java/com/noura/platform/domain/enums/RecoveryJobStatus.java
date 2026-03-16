package com.noura.platform.domain.enums;

/**
 * Enumerates bulk destructive-action job lifecycle states.
 */
public enum RecoveryJobStatus {
    VALIDATING,
    QUEUED,
    CANCEL_REQUESTED,
    RUNNING,
    CANCELLED,
    COMPLETED,
    PARTIAL_SUCCESS,
    FAILED
}
