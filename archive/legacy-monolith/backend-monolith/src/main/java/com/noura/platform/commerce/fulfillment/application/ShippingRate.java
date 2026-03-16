package com.noura.platform.commerce.fulfillment.application;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Shipping rate option from a carrier.
 */
public record ShippingRate(
        String carrierId,
        String serviceCode,
        String serviceName,
        BigDecimal totalPrice,
        String currency,
        String estimatedTransit,
        LocalDateTime estimatedDelivery
) {
}
