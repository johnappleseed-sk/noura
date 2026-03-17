package com.noura.shipping.dto.shipping;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Quote result for one selected shipping method.
 *
 * @param carrierCode carrier code
 * @param methodCode method code
 * @param methodName method display name
 * @param amount quote amount
 * @param currencyCode quote currency code
 * @param estimatedDaysMin minimum estimated transit days
 * @param estimatedDaysMax maximum estimated transit days
 * @param estimatedDeliveryAt projected delivery timestamp
 * @param ruleSummary quote explanation
 * @param quotedAt quote generation timestamp
 */
public record ShippingQuoteResponse(
        String carrierCode,
        String methodCode,
        String methodName,
        BigDecimal amount,
        String currencyCode,
        Integer estimatedDaysMin,
        Integer estimatedDaysMax,
        Instant estimatedDeliveryAt,
        String ruleSummary,
        Instant quotedAt
) {
}
