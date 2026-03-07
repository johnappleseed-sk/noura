package com.noura.platform.commerce.fulfillment.application;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Abstract interface for shipping carrier integrations.
 * Implement this interface for each carrier (FedEx, UPS, local couriers, etc.).
 */
public interface ShippingCarrier {

    /**
     * Get the carrier identifier (e.g., "fedex", "ups", "dhl").
     */
    String getCarrierId();

    /**
     * Get the display name for this carrier.
     */
    String getDisplayName();

    /**
     * Check if this carrier is enabled and configured.
     */
    boolean isEnabled();

    /**
     * Get available shipping rates for a package.
     *
     * @param request Rate request containing package details and destination
     * @return List of available shipping options with prices
     */
    List<ShippingRate> getRates(RateRequest request);

    /**
     * Create a shipment and get a shipping label.
     *
     * @param request Shipment creation request
     * @return Result containing tracking number and label URL
     */
    ShipmentResult createShipment(CreateShipmentRequest request);

    /**
     * Get tracking information for a shipment.
     *
     * @param trackingNumber The carrier's tracking number
     * @return Current tracking status and events
     */
    TrackingInfo getTracking(String trackingNumber);

    /**
     * Cancel a shipment (if supported and not yet shipped).
     *
     * @param trackingNumber The carrier's tracking number
     * @return Result of the cancellation
     */
    CancelResult cancelShipment(String trackingNumber);

    // ===============================
    // Request/Response Records
    // ===============================

    record RateRequest(
            Address fromAddress,
            Address toAddress,
            List<PackageInfo> packages,
            String serviceType // Optional: filter to specific service
    ) {}

    record Address(
            String name,
            String company,
            String line1,
            String line2,
            String city,
            String stateProvince,
            String postalCode,
            String countryCode,
            String phone,
            String email
    ) {}

    record PackageInfo(
            BigDecimal weightKg,
            BigDecimal lengthCm,
            BigDecimal widthCm,
            BigDecimal heightCm,
            BigDecimal declaredValue,
            String currencyCode
    ) {}

    record ShippingRate(
            String serviceCode,
            String serviceName,
            BigDecimal amount,
            String currencyCode,
            int estimatedDaysMin,
            int estimatedDaysMax,
            LocalDateTime estimatedDelivery
    ) {}

    record CreateShipmentRequest(
            Address fromAddress,
            Address toAddress,
            List<PackageInfo> packages,
            String serviceCode,
            String reference,
            boolean signatureRequired,
            boolean saturdayDelivery
    ) {}

    record ShipmentResult(
            boolean success,
            String trackingNumber,
            String labelUrl,
            byte[] labelPdf,
            BigDecimal cost,
            String currencyCode,
            LocalDateTime estimatedDelivery,
            String errorCode,
            String errorMessage
    ) {
        public static ShipmentResult success(String trackingNumber, String labelUrl,
                                            BigDecimal cost, String currencyCode,
                                            LocalDateTime estimatedDelivery) {
            return new ShipmentResult(true, trackingNumber, labelUrl, null,
                    cost, currencyCode, estimatedDelivery, null, null);
        }

        public static ShipmentResult failure(String errorCode, String errorMessage) {
            return new ShipmentResult(false, null, null, null,
                    null, null, null, errorCode, errorMessage);
        }
    }

    record TrackingInfo(
            String trackingNumber,
            TrackingStatus status,
            String statusDescription,
            LocalDateTime estimatedDelivery,
            LocalDateTime deliveredAt,
            String signedBy,
            List<TrackingEvent> events
    ) {}

    record TrackingEvent(
            LocalDateTime timestamp,
            String location,
            String description,
            TrackingStatus status
    ) {}

    enum TrackingStatus {
        UNKNOWN,
        LABEL_CREATED,
        PICKED_UP,
        IN_TRANSIT,
        OUT_FOR_DELIVERY,
        DELIVERED,
        EXCEPTION,
        RETURNED,
        CANCELLED
    }

    record CancelResult(
            boolean success,
            String message
    ) {}
}
