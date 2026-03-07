package com.noura.platform.commerce.fulfillment.application;

/**
 * Result of cancelling a shipment.
 */
public record CancelResult(
        boolean success,
        String trackingNumber,
        String errorCode,
        String errorMessage
) {
    public static CancelResult success(String trackingNumber) {
        return new CancelResult(true, trackingNumber, null, null);
    }

    public static CancelResult failure(String errorCode, String errorMessage) {
        return new CancelResult(false, null, errorCode, errorMessage);
    }
}
