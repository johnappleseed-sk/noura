package com.noura.platform.commerce.b2b.domain;

/**
 * Status of a B2B company account.
 */
public enum CompanyStatus {
    /** Pending admin approval */
    PENDING_APPROVAL,

    /** Active and can place orders */
    ACTIVE,

    /** Suspended (payment issues, etc.) */
    SUSPENDED,

    /** Account closed */
    CLOSED
}
