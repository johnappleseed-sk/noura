package com.noura.customer.domain.enums;

/**
 * Represents validation confidence for customer shipping/billing addresses.
 */
public enum AddressValidationStatus {
    /**
     * Address has not been validated against delivery coverage.
     */
    UNVERIFIED,
    /**
     * Address is valid for delivery at verification time.
     */
    VALID,
    /**
     * Address lies outside configured service areas.
     */
    OUT_OF_SERVICE_AREA,
    /**
     * Address lies outside nearby store delivery radius.
     */
    OUT_OF_STORE_RADIUS,
    /**
     * No currently available store could serve the address.
     */
    STORE_UNAVAILABLE,
    /**
     * Store exists but is closed for requested operation window.
     */
    STORE_CLOSED
}
