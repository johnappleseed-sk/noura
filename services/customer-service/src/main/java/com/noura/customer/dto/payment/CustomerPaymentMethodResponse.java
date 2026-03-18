package com.noura.customer.dto.payment;

import java.util.UUID;

/**
 * Account-scoped payment method response.
 *
 * @param id payment method identifier
 * @param methodType payment method type code
 * @param provider provider code or display name
 * @param tokenizedReference tokenized or masked provider reference
 * @param defaultMethod whether this payment method is the customer's default
 */
public record CustomerPaymentMethodResponse(
        UUID id,
        String methodType,
        String provider,
        String tokenizedReference,
        boolean defaultMethod
) {
}
