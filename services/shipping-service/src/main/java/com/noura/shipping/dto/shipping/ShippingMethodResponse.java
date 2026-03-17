package com.noura.shipping.dto.shipping;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Shipping method option returned by method discovery.
 *
 * @param carrierCode carrier code
 * @param methodCode method code
 * @param methodName method display name
 * @param amount quoted amount
 * @param currencyCode quote currency code
 * @param estimatedDaysMin minimum estimated transit days
 * @param estimatedDaysMax maximum estimated transit days
 * @param estimatedDeliveryAt projected delivery timestamp
 * @param supportsTracking whether tracking is supported
 * @param ruleSummary short explanation of the rule-based quote
 */
public record ShippingMethodResponse(
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
