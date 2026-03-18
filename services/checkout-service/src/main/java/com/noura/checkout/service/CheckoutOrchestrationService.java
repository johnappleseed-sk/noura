package com.noura.checkout.service;

import com.noura.checkout.dto.checkout.CheckoutPlaceOrderRequest;
import com.noura.checkout.dto.checkout.CheckoutPlaceOrderResponse;
import com.noura.checkout.dto.checkout.CheckoutPreviewRequest;
import com.noura.checkout.dto.checkout.CheckoutPreviewResponse;
import com.noura.checkout.dto.checkout.CheckoutValidateRequest;
import com.noura.checkout.dto.checkout.CheckoutValidationResponse;
import com.noura.checkout.service.model.CheckoutRequestContext;

/**
 * Checkout orchestration use cases.
 */
public interface CheckoutOrchestrationService {

    /**
     * Builds checkout preview snapshot from cart, pricing, inventory, and optional address data.
     *
     * @param context request-level checkout actor context
     * @param request preview request payload
     * @return checkout preview response
     */
    CheckoutPreviewResponse preview(CheckoutRequestContext context, CheckoutPreviewRequest request);

    /**
     * Validates checkout readiness for place-order.
     *
     * @param context request-level checkout actor context
     * @param request validate request payload
     * @return checkout validation response
     */
    CheckoutValidationResponse validate(CheckoutRequestContext context, CheckoutValidateRequest request);

    /**
     * Places an order using validated checkout data with reservation rollback protections.
     *
     * @param context request-level checkout actor context
     * @param request place-order request payload
     * @param idempotencyKeyHeader optional idempotency key header value
     * @return place-order response
     */
    CheckoutPlaceOrderResponse placeOrder(
            CheckoutRequestContext context,
            CheckoutPlaceOrderRequest request,
            String idempotencyKeyHeader
    );
}

