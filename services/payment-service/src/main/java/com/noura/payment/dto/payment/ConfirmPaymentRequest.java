package com.noura.payment.dto.payment;

import com.noura.payment.domain.enums.PaymentConfirmationAction;
import jakarta.validation.constraints.Size;

/**
 * Command payload for confirming a payment intent.
 *
 * @param action confirmation action
 * @param providerTransactionId optional provider transaction reference override
 * @param providerReference optional provider-side reference
 */
public record ConfirmPaymentRequest(
        PaymentConfirmationAction action,
        @Size(max = 128) String providerTransactionId,
        @Size(max = 128) String providerReference
) {

    /**
     * Resolves confirmation action with default.
     *
     * @return resolved action
     */
    public PaymentConfirmationAction resolvedAction() {
        return action == null ? PaymentConfirmationAction.CAPTURE : action;
    }
}
