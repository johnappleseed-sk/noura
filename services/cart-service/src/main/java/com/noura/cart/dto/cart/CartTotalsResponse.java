package com.noura.cart.dto.cart;

import java.math.BigDecimal;

/**
 * Cart totals read model.
 *
 * @param subtotal subtotal amount
 * @param discountAmount discount amount (v1 placeholder)
 * @param shippingAmount shipping amount (v1 placeholder)
 * @param totalAmount grand total amount
 * @param couponCode applied coupon code placeholder
 */
public record CartTotalsResponse(
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal shippingAmount,
        BigDecimal totalAmount,
        String couponCode
) {
}
