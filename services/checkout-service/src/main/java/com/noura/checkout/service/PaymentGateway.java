package com.noura.checkout.service;

import com.noura.checkout.dto.checkout.CheckoutPlaceOrderRequest;
import com.noura.checkout.dto.checkout.CheckoutPaymentSummaryResponse;
import com.noura.checkout.service.model.CheckoutRequestContext;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Payment abstraction for checkout orchestration.
 */
public interface PaymentGateway {

    /**
     * Creates and confirms a payment for a newly created order.
     *
     * @param context request context
     * @param request place-order request
     * @param orderId created order identifier
     * @param currencyCode immutable order currency code
     * @param totalAmount immutable order total amount
     * @param idempotencyKey optional checkout idempotency key
     * @return normalized payment summary
     */
    CheckoutPaymentSummaryResponse createAndConfirmPayment(
            CheckoutRequestContext context,
            CheckoutPlaceOrderRequest request,
            UUID orderId,
            String currencyCode,
            BigDecimal totalAmount,
            String idempotencyKey
    );
}
