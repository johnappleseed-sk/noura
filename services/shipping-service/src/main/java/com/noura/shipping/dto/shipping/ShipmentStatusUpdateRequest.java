package com.noura.shipping.dto.shipping;

import com.noura.shipping.domain.enums.ShipmentStatus;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Trusted internal shipment status update payload.
 *
 * @param shipmentId shipment identifier
 * @param status next shipment status
 * @param externalShipmentId optional carrier shipment identifier
 * @param trackingNumber optional tracking number
 * @param trackingUrl optional tracking URL
 * @param estimatedDeliveryAt optional estimated delivery time
 * @param failureReason optional failure or exception reason
 * @param source status source label for audit notes
 * @param metadata optional metadata patch to merge into shipment metadata
 * @param eventTimestamp optional source event timestamp
 */
public record ShipmentStatusUpdateRequest(
        @NotNull(message = "shipmentId is required")
        UUID shipmentId,
        @NotNull(message = "status is required")
        ShipmentStatus status,
        String externalShipmentId,
        String trackingNumber,
        String trackingUrl,
        Instant estimatedDeliveryAt,
        String failureReason,
        String source,
        Map<String, Object> metadata,
        Instant eventTimestamp
) {
}
