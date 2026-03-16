package com.noura.platform.commerce.fulfillment.application;

/**
 * Result of creating a shipment.
 */
public record ShipmentResult(
        boolean success,
        String trackingNumber,
        String labelUrl,
        String rawResponse,
        String errorCode,
        String errorMessage
) {
    public static ShipmentResult success(String trackingNumber, String labelUrl, String rawResponse) {
        return new ShipmentResult(true, trackingNumber, labelUrl, rawResponse, null, null);
    }

    public static ShipmentResult failure(String errorCode, String errorMessage) {
        return new ShipmentResult(false, null, null, null, errorCode, errorMessage);
    }
}
