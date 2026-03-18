package com.noura.order.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Checkout-to-order command payload used to create immutable order records.
 *
 * @param customerRef optional customer reference for internal calls
 * @param storeId store identifier
 * @param addressId address identifier
 * @param currencyCode currency code
 * @param paymentReference payment reference
 * @param couponCode coupon code
 * @param shippingAddress structured shipping address snapshot
 * @param billingAddress structured billing address snapshot
 * @param shippingAddressSnapshot plain-text shipping snapshot fallback
 * @param checkoutContext additional checkout snapshot fields
 * @param subtotal order subtotal
 * @param discountAmount order discount amount
 * @param shippingAmount order shipping amount
 * @param taxAmount order tax amount
 * @param totalAmount order total amount
 * @param paymentConfirmed indicates payment was confirmed before creation
 * @param idempotencyKey idempotency key for deterministic retries
 * @param items immutable order line payloads
 */
public record CreateOrderRequest(
        @Size(max = 180) String customerRef,
        UUID storeId,
        UUID addressId,
        @NotBlank @Size(max = 8) String currencyCode,
        @Size(max = 255) String paymentReference,
        @Size(max = 80) String couponCode,
        @Valid AddressSnapshotDto shippingAddress,
        @Valid AddressSnapshotDto billingAddress,
        String shippingAddressSnapshot,
        Map<String, Object> checkoutContext,
        @NotNull @DecimalMin("0.0") BigDecimal subtotal,
        @NotNull @DecimalMin("0.0") BigDecimal discountAmount,
        @NotNull @DecimalMin("0.0") BigDecimal shippingAmount,
        @NotNull @DecimalMin("0.0") BigDecimal taxAmount,
        @NotNull @DecimalMin("0.0") BigDecimal totalAmount,
        Boolean paymentConfirmed,
        @Size(max = 128) String idempotencyKey,
        @NotEmpty List<@Valid CreateOrderItemRequest> items
) {
}

