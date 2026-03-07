package com.noura.platform.commerce.fulfillment.application;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Tracking information for a shipment.
 */
public record TrackingInfo(
        String trackingNumber,
        String carrierId,
        TrackingStatus status,
        String statusDescription,
        List<TrackingEvent> events,
        LocalDateTime estimatedDelivery
) {
    public static TrackingInfo notFound(String trackingNumber) {
        return new TrackingInfo(
                trackingNumber,
                null,
                TrackingStatus.UNKNOWN,
                "Tracking number not found",
                Collections.emptyList(),
                null
        );
    }

    public static TrackingInfo error(String trackingNumber, String message) {
        return new TrackingInfo(
                trackingNumber,
                null,
                TrackingStatus.UNKNOWN,
                "Error: " + message,
                Collections.emptyList(),
                null
        );
    }
}
