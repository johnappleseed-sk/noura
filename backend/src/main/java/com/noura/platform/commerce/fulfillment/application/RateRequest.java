package com.noura.platform.commerce.fulfillment.application;

import java.math.BigDecimal;

/**
 * Request for shipping rates.
 */
public record RateRequest(
        Address originAddress,
        Address destinationAddress,
        BigDecimal weightLbs,
        int lengthIn,
        int widthIn,
        int heightIn
) {
}
