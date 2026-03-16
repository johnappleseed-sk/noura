package com.noura.platform.commerce.fulfillment.application;

import java.math.BigDecimal;

/**
 * Request to create a shipment and generate a label.
 */
public record CreateShipmentRequest(
        String orderId,
        ContactAddress shipper,
        ContactAddress recipient,
        String serviceType,
        BigDecimal weightLbs,
        int lengthIn,
        int widthIn,
        int heightIn,
        String reference
) {
}
