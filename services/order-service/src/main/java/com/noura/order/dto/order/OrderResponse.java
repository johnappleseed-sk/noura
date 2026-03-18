package com.noura.order.dto.order;

import com.noura.order.domain.enums.OrderStatus;
import com.noura.order.domain.enums.RefundStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Order response DTO for storefront and admin consumers.
 *
 * @param id order identifier
 * @param orderNumber business order number
 * @param userId customer identifier alias for legacy admin UI compatibility
 * @param customerRef customer reference
 * @param storeId store identifier
 * @param addressId address identifier
 * @param subtotal subtotal amount
 * @param discountAmount discount amount
 * @param shippingAmount shipping amount
 * @param taxAmount tax amount
 * @param totalAmount total amount
 * @param currencyCode currency code
 * @param status order status
 * @param refundStatus refund status
 * @param paymentReference payment reference
 * @param couponCode coupon code
 * @param shippingAddress shipping snapshot object or string
 * @param billingAddress billing snapshot object or string
 * @param createdAt creation timestamp
 * @param updatedAt update timestamp
 * @param placedAt placement timestamp
 * @param items line items
 */
public record OrderResponse(
        UUID id,
        String orderNumber,
        String userId,
        String customerRef,
        UUID storeId,
        UUID addressId,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal shippingAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String currencyCode,
        OrderStatus status,
        RefundStatus refundStatus,
        String paymentReference,
        String couponCode,
        Object shippingAddress,
        Object billingAddress,
        Instant createdAt,
        Instant updatedAt,
        Instant placedAt,
        List<OrderItemResponse> items
) {
}

