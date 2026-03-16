package com.noura.platform.domain.enums;

/**
 * Product submission workflow state for store -> super admin review.
 */
public enum ProductSubmissionStatus {
    PENDING_REVIEW,
    REVISION_REQUESTED,
    APPROVED,
    REJECTED
}

