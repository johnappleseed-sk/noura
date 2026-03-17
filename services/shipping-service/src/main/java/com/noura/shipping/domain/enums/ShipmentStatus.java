package com.noura.shipping.domain.enums;

/**
 * Shipment lifecycle states owned by shipping-service.
 */
public enum ShipmentStatus {
    /**
     * Internal shipment record created before handoff progresses.
     */
    CREATED,
    /**
     * Carrier label or booking created.
     */
    LABEL_CREATED,
    /**
     * Order is ready for warehouse or courier handoff.
     */
    READY_FOR_FULFILLMENT,
    /**
     * Shipment is moving through the carrier network.
     */
    IN_TRANSIT,
    /**
     * Shipment is with the last-mile courier for delivery.
     */
    OUT_FOR_DELIVERY,
    /**
     * Delivery completed successfully.
     */
    DELIVERED,
    /**
     * Carrier or warehouse encountered an operational exception.
     */
    EXCEPTION,
    /**
     * Shipment returned after dispatch or delivery attempt.
     */
    RETURNED,
    /**
     * Shipment was canceled before completion.
     */
    CANCELLED
}
