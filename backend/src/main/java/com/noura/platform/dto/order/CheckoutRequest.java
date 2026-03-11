package com.noura.platform.dto.order;

import com.noura.platform.domain.enums.FulfillmentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CheckoutRequest(
        @NotNull FulfillmentMethod fulfillmentMethod,
        UUID storeId,
        UUID addressId,
        String shippingAddressSnapshot,
        String paymentReference,
        String couponCode,
        boolean b2bInvoice,
        @Size(max = 128) String idempotencyKey
) {
}
