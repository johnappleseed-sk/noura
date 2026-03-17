package com.noura.payment.service;

import com.noura.payment.dto.payment.ConfirmPaymentRequest;
import com.noura.payment.dto.payment.CreatePaymentIntentRequest;
import com.noura.payment.dto.payment.PaymentResponse;
import com.noura.payment.dto.payment.PaymentStatusUpdateRequest;
import com.noura.payment.dto.payment.PaymentWebhookResponse;
import com.noura.payment.service.model.PaymentRequestContext;

import java.util.Map;
import java.util.UUID;

/**
 * Payment command/query service contract.
 */
public interface PaymentService {

    /**
     * Creates one internal payment intent tied to an existing order.
     *
     * @param context request actor context
     * @param request create-intent payload
     * @return persisted payment response
     */
    PaymentResponse createPaymentIntent(PaymentRequestContext context, CreatePaymentIntentRequest request);

    /**
     * Confirms one payment intent using authorize or capture semantics.
     *
     * @param context request actor context
     * @param paymentId payment identifier
     * @param request confirm payload
     * @return updated payment response
     */
    PaymentResponse confirmPayment(PaymentRequestContext context, UUID paymentId, ConfirmPaymentRequest request);

    /**
     * Retrieves one payment by identifier while enforcing ownership/admin access.
     *
     * @param context request actor context
     * @param paymentId payment identifier
     * @return payment response
     */
    PaymentResponse getPaymentById(PaymentRequestContext context, UUID paymentId);

    /**
     * Retrieves the latest payment associated with one order.
     *
     * @param context request actor context
     * @param orderId order identifier
     * @return latest payment response
     */
    PaymentResponse getLatestPaymentByOrderId(PaymentRequestContext context, UUID orderId);

    /**
     * Applies an internal/manual status update to one payment.
     *
     * @param request status update payload
     * @param actorId audit actor identifier
     * @return updated payment response
     */
    PaymentResponse updatePaymentStatus(PaymentStatusUpdateRequest request, String actorId);

    /**
     * Handles one inbound provider webhook.
     *
     * @param providerCode provider code
     * @param payload raw request payload
     * @param headers normalized request headers
     * @return webhook processing response
     */
    PaymentWebhookResponse handleWebhook(String providerCode, String payload, Map<String, String> headers);
}
