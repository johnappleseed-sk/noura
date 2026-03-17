package com.noura.shipping.provider;

import com.noura.shipping.domain.enums.ShipmentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Abstraction boundary for carrier-specific shipping behavior.
 */
public interface ShippingCarrier {

    /**
     * Returns the primary carrier code handled by this adapter.
     *
     * @return carrier code
     */
    String carrierCode();

    /**
     * Returns the human-readable carrier display name.
     *
     * @return carrier display name
     */
    String displayName();

    /**
     * Returns whether this adapter supports a requested carrier code.
     *
     * @param requestedCarrierCode requested carrier code
     * @return {@code true} when supported
     */
    default boolean supports(String requestedCarrierCode) {
        if (requestedCarrierCode == null || requestedCarrierCode.isBlank()) {
            return false;
        }
        return carrierCode().equalsIgnoreCase(requestedCarrierCode.trim());
    }

    /**
     * Lists available shipping methods for one destination/cart snapshot.
     *
     * @param request method discovery request
     * @return available shipping methods
     */
    List<AvailableMethod> listAvailableMethods(MethodRequest request);

    /**
     * Quotes one selected shipping method.
     *
     * @param request quote request
     * @return quote result
     */
    QuoteResult quote(QuoteRequest request);

    /**
     * Creates a shipment with carrier-facing identifiers and tracking data.
     *
     * @param request shipment creation request
     * @return creation result
     */
    ShipmentCreationResult createShipment(CreateShipmentCommand request);

    /**
     * Fetches current carrier shipment status.
     *
     * @param request carrier status request
     * @return normalized tracking result
     */
    TrackingResult fetchShipmentStatus(StatusRequest request);

    /**
     * Address shape used by carrier adapters.
     */
    record ShippingAddress(
            String fullName,
            String phone,
            String line1,
            String line2,
            String district,
            String city,
            String stateProvince,
            String postalCode,
            String countryCode
    ) {
    }

    /**
     * Parcel summary used by carrier adapters.
     */
    record Parcel(
            Integer quantity,
            BigDecimal weightKg,
            BigDecimal lengthCm,
            BigDecimal widthCm,
            BigDecimal heightCm
    ) {
    }

    /**
     * Method discovery request.
     */
    record MethodRequest(
            ShippingAddress address,
            BigDecimal cartSubtotal,
            String currencyCode,
            Integer itemCount,
            BigDecimal totalWeightKg,
            Map<String, Object> metadata
    ) {
    }

    /**
     * Quote request.
     */
    record QuoteRequest(
            ShippingAddress address,
            BigDecimal cartSubtotal,
            String currencyCode,
            Integer itemCount,
            BigDecimal totalWeightKg,
            String methodCode,
            Map<String, Object> metadata
    ) {
    }

    /**
     * Shipment creation request.
     */
    record CreateShipmentCommand(
            UUID shipmentId,
            UUID orderId,
            String orderNumber,
            String shipmentReference,
            ShippingAddress recipientAddress,
            List<Parcel> parcels,
            BigDecimal cartSubtotal,
            String currencyCode,
            String methodCode,
            boolean signatureRequired,
            Map<String, Object> metadata
    ) {
    }

    /**
     * Carrier status request.
     */
    record StatusRequest(
            UUID shipmentId,
            String shipmentReference,
            String externalShipmentId,
            String trackingNumber,
            ShipmentStatus currentStatus,
            Map<String, Object> metadata
    ) {
    }

    /**
     * Available shipping method.
     */
    record AvailableMethod(
            String carrierCode,
            String methodCode,
            String methodName,
            BigDecimal amount,
            String currencyCode,
            Integer estimatedDaysMin,
            Integer estimatedDaysMax,
            Instant estimatedDeliveryAt,
            boolean supportsTracking,
            String ruleSummary
    ) {
    }

    /**
     * Quote result for a selected method.
     */
    record QuoteResult(
            String carrierCode,
            String methodCode,
            String methodName,
            BigDecimal amount,
            String currencyCode,
            Integer estimatedDaysMin,
            Integer estimatedDaysMax,
            Instant estimatedDeliveryAt,
            String ruleSummary
    ) {
    }

    /**
     * Shipment creation result.
     */
    record ShipmentCreationResult(
            ShipmentStatus status,
            String externalShipmentId,
            String trackingNumber,
            String trackingUrl,
            Instant estimatedDeliveryAt,
            Instant labelCreatedAt,
            String failureReason
    ) {
    }

    /**
     * Carrier tracking result normalized to internal shipment state.
     */
    record TrackingResult(
            ShipmentStatus status,
            String trackingNumber,
            String trackingUrl,
            Instant estimatedDeliveryAt,
            Instant deliveredAt,
            String failureReason
    ) {
    }
}
