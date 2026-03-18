package com.noura.checkout.service;

import com.noura.checkout.dto.checkout.CheckoutPlaceOrderRequest;
import com.noura.checkout.dto.checkout.CheckoutPlaceOrderResponse;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistent idempotency service for checkout place-order commands.
 */
public interface CheckoutIdempotencyService {

    /**
     * Attempts to replay an already successful response for a customer/key pair.
     *
     * @param customerRef customer reference
     * @param idempotencyKey normalized idempotency key
     * @return optional replay response
     */
    Optional<CheckoutPlaceOrderResponse> tryReplay(String customerRef, String idempotencyKey);

    /**
     * Marks a customer/key pair as processing and returns record ID.
     *
     * @param customerRef customer reference
     * @param idempotencyKey normalized idempotency key
     * @param request place-order request payload
     * @param actor actor identifier
     * @return idempotency record identifier
     */
    UUID beginProcessing(
            String customerRef,
            String idempotencyKey,
            CheckoutPlaceOrderRequest request,
            String actor
    );

    /**
     * Marks one idempotency record as successfully completed.
     *
     * @param recordId record identifier
     * @param response place-order response payload
     * @param actor actor identifier
     */
    void markSuccess(UUID recordId, CheckoutPlaceOrderResponse response, String actor);

    /**
     * Marks one idempotency record as failed.
     *
     * @param recordId record identifier
     * @param errorCode failure code
     * @param errorMessage failure detail
     * @param actor actor identifier
     */
    void markFailure(UUID recordId, String errorCode, String errorMessage, String actor);
}

