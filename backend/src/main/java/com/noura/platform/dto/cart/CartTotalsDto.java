package com.noura.platform.dto.cart;

import java.math.BigDecimal;

public record CartTotalsDto(
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal shippingAmount,
        BigDecimal totalAmount,
        String couponCode
) {
}
