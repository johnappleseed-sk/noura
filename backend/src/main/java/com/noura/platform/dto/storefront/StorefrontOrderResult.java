package com.noura.platform.dto.storefront;

import com.noura.platform.dto.payment.PaymentTransactionResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record StorefrontOrderResult(
        Long id,
        String orderNumber,
        String status,
        String currencyCode,
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal taxTotal,
        BigDecimal shippingTotal,
        BigDecimal grandTotal,
        LocalDateTime placedAt,
        StorefrontOrderShippingAddressDto shippingAddress,
        PaymentTransactionResult latestPayment,
        List<StorefrontOrderItemDto> items
) {
}
