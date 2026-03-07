package com.noura.platform.commerce.notifications.domain;

/**
 * Type of notification for categorization and template selection.
 */
public enum NotificationType {
    // Order lifecycle
    ORDER_CONFIRMATION,
    ORDER_PAYMENT_RECEIVED,
    ORDER_SHIPPED,
    ORDER_DELIVERED,
    ORDER_CANCELLED,
    ORDER_REFUNDED,

    // Customer account
    CUSTOMER_WELCOME,
    CUSTOMER_PASSWORD_RESET,
    CUSTOMER_EMAIL_VERIFICATION,

    // Returns
    RETURN_REQUEST_RECEIVED,
    RETURN_APPROVED,
    RETURN_REJECTED,
    RETURN_REFUND_ISSUED,

    // Marketing
    PROMOTIONAL,
    ABANDONED_CART,

    // System
    SYSTEM_ALERT
}
