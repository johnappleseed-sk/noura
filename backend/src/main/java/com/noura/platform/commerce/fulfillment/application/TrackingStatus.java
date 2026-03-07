package com.noura.platform.commerce.fulfillment.application;

/**
 * Standardized tracking status across all shipping carriers.
 */
public enum TrackingStatus {
    /** Shipment label created but not yet picked up */
    PENDING,

    /** Package has been picked up by carrier */
    PICKED_UP,

    /** Package is in transit */
    IN_TRANSIT,

    /** Package is out for delivery */
    OUT_FOR_DELIVERY,

    /** Package has been delivered */
    DELIVERED,

    /** Delivery attempted but failed */
    DELIVERY_ATTEMPTED,

    /** Package returned to sender */
    RETURNED,

    /** Exception occurred (delay, damage, etc.) */
    EXCEPTION,

    /** Package is on hold */
    ON_HOLD,

    /** Shipment was cancelled */
    CANCELLED,

    /** Status unknown or not found */
    UNKNOWN
}
