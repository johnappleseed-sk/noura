package com.noura.shipping.domain.enums;

/**
 * Downstream fulfillment hook hints derived from shipment status.
 */
public enum FulfillmentHookType {
    /**
     * No downstream fulfillment hook is necessary.
     */
    NONE,
    /**
     * Shipment record exists and the order is effectively packed/ready.
     */
    ORDER_PACKED,
    /**
     * Shipment has left fulfillment and should map to an order shipped event.
     */
    ORDER_SHIPPED,
    /**
     * Shipment completed delivery.
     */
    ORDER_DELIVERED,
    /**
     * Shipment hit a fulfillment exception.
     */
    SHIPMENT_EXCEPTION,
    /**
     * Shipment was canceled before completion.
     */
    SHIPMENT_CANCELLED,
    /**
     * Shipment entered a returned state.
     */
    SHIPMENT_RETURNED
}
