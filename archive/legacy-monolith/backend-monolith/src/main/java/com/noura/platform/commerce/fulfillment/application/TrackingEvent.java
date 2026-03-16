package com.noura.platform.commerce.fulfillment.application;

/**
 * Individual tracking event/scan from a shipping carrier.
 */
public record TrackingEvent(
        String timestamp,
        String description,
        String location
) {
}
