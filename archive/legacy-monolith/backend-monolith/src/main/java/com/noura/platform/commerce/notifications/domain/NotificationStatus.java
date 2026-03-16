package com.noura.platform.commerce.notifications.domain;

/**
 * Status of a notification delivery attempt.
 */
public enum NotificationStatus {
    PENDING,
    SENT,
    DELIVERED,
    FAILED,
    BOUNCED,
    CANCELLED
}
