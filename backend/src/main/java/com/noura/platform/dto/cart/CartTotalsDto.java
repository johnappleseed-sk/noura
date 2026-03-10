package com.noura.platform.dto.cart;

import java.math.BigDecimal;
import java.util.List;

public record CartTotalsDto(
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal shippingAmount,
        BigDecimal totalAmount,
        String couponCode,
        List<String> appliedPromotionCodes,
        boolean freeShippingApplied
) {
}
