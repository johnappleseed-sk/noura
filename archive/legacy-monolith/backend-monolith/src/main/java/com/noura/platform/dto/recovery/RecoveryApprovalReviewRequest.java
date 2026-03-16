package com.noura.platform.dto.recovery;

/**
 * Defines a review payload used to approve or reject recovery approval requests.
 *
 * @param reviewerNotes Optional reviewer notes captured for compliance.
 */
public record RecoveryApprovalReviewRequest(String reviewerNotes) {
}

