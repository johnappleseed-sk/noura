package com.noura.platform.commerce.marketplace.domain;

/**
 * Status of a product listing on an external marketplace.
 */
public enum ListingStatus {
    /** Pending submission to marketplace */
    PENDING,

    /** Submitted, awaiting approval */
    SUBMITTED,

    /** Active and visible on marketplace */
    ACTIVE,

    /** Inactive/disabled */
    INACTIVE,

    /** Rejected by marketplace */
    REJECTED,

    /** Sync error */
    ERROR,

    /** Paused by seller */
    PAUSED
}
