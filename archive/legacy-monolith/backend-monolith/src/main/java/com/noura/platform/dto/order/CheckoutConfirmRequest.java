package com.noura.platform.dto.order;

import com.noura.platform.domain.enums.FulfillmentMethod;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Confirm step payload that can be merged with a persisted checkout draft.
 */
public record CheckoutConfirmRequest(
        FulfillmentMethod fulfillmentMethod,
        UUID storeId,
        UUID addressId,
        String shippingAddressSnapshot,
        String paymentReference,
        String couponCode,
        Boolean b2bInvoice,
        @Size(max = 128) String idempotencyKey
) {
}
