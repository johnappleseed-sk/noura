package com.noura.shipping.dto.shipping;

import com.noura.shipping.domain.enums.FulfillmentHookType;
import com.noura.shipping.domain.enums.ShipmentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Shipment response payload for create and read APIs.
 *
 * @param id shipment identifier
 * @param orderId order identifier
 * @param orderNumber business order number
 * @param customerRef customer reference
 * @param shipmentReference shipment reference
 * @param carrierCode carrier code
 * @param methodCode method code
 * @param methodName method display name
 * @param status current shipment status
 * @param fulfillmentHook derived downstream fulfillment hook
 * @param quotedAmount shipment quote amount
 * @param currencyCode currency code
 * @param externalShipmentId carrier shipment identifier
 * @param trackingNumber tracking number
 * @param trackingUrl tracking URL
 * @param estimatedDeliveryAt projected delivery timestamp
 * @param labelCreatedAt label creation timestamp
 * @param shippedAt shipped timestamp
 * @param deliveredAt delivered timestamp
 * @param lastStatusUpdateAt last status update timestamp
 * @param lastCarrierSyncAt last carrier sync timestamp
 * @param failureReason failure or exception reason
 * @param recipientAddress recipient address snapshot
 * @param parcels parcel summary
 * @param metadata metadata map
 * @param createdAt creation timestamp
 * @param updatedAt update timestamp
 */
public record ShipmentResponse(
        UUID id,
        UUID orderId,
        String orderNumber,
        String customerRef,
        String shipmentReference,
        String carrierCode,
        String methodCode,
        String methodName,
        ShipmentStatus status,
        FulfillmentHookType fulfillmentHook,
        BigDecimal quotedAmount,
        String currencyCode,
        String externalShipmentId,
        String trackingNumber,
        String trackingUrl,
        Instant estimatedDeliveryAt,
        Instant labelCreatedAt,
        Instant shippedAt,
        Instant deliveredAt,
        Instant lastStatusUpdateAt,
        Instant lastCarrierSyncAt,
        String failureReason,
        AddressRequest recipientAddress,
        List<ParcelRequest> parcels,
        Map<String, Object> metadata,
        Instant createdAt,
        Instant updatedAt
) {
}
