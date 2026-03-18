package com.noura.checkout.dto.checkout;

import java.math.BigDecimal;

/**
 * Checkout totals summary.
 *
 * @param subtotal subtotal amount
 * @param discountAmount discount amount
 * @param shippingAmount shipping amount
 * @param taxAmount tax amount
 * @param totalAmount grand total amount
 * @param currencyCode currency code
 */
public record CheckoutTotalsResponse(
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal shippingAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String currencyCode
) {
}

