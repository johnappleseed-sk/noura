package com.noura.customer.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Account-scoped payment method upsert payload.
 *
 * @param methodType payment method type code such as {@code CARD} or {@code CASH_ON_DELIVERY}
 * @param provider provider code or display name
 * @param tokenizedReference tokenized or masked provider reference
 * @param defaultMethod whether this payment method should become the customer's default
 */
public record CustomerPaymentMethodRequest(
        @NotBlank @Size(max = 40) String methodType,
        @NotBlank @Size(max = 80) String provider,
        @NotBlank @Size(max = 255) String tokenizedReference,
        Boolean defaultMethod
) {
}
