package com.noura.review.domain.enums;

/**
 * Moderation workflow status for one product review.
 */
public enum ReviewModerationStatus {
    /**
     * Review is awaiting moderator action and is not visible publicly.
     */
    PENDING,
    /**
     * Review is approved and visible in public product review lists.
     */
    APPROVED,
    /**
     * Review is rejected and excluded from public product review lists.
     */
    REJECTED
}
