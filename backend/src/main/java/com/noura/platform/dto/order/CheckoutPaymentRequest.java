package com.noura.platform.dto.order;

import jakarta.validation.constraints.Size;

/**
 * Captures payment step inputs for a multi-step checkout draft.
 */
public record CheckoutPaymentRequest(
        String paymentReference,
        String couponCode,
        boolean b2bInvoice,
        @Size(max = 128) String idempotencyKey
) {
}
