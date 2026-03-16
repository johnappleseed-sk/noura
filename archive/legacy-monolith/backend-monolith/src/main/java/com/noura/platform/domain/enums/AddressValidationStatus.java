package com.noura.platform.domain.enums;

/**
 * Represents the backend's current confidence in whether a saved address can be used for delivery.
 */
public enum AddressValidationStatus {
    UNVERIFIED,
    VALID,
    OUT_OF_SERVICE_AREA,
    OUT_OF_STORE_RADIUS,
    STORE_UNAVAILABLE,
    STORE_CLOSED
}
