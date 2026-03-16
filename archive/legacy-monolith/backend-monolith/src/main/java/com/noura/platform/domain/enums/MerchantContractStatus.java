package com.noura.platform.domain.enums;

/**
 * Contract lifecycle status used to gate store activation.
 */
public enum MerchantContractStatus {
    DRAFT,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    SUSPENDED,
    TERMINATED
}

