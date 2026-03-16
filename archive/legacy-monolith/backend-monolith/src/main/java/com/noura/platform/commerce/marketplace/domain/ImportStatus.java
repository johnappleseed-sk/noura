package com.noura.platform.commerce.marketplace.domain;

/**
 * Status of importing a marketplace order.
 */
public enum ImportStatus {
    /** Pending import */
    PENDING,

    /** Successfully imported to internal order */
    IMPORTED,

    /** Import failed */
    FAILED,

    /** Skipped (duplicate, test order, etc.) */
    SKIPPED
}
