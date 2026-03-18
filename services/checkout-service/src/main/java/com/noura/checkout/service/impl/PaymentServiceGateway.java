package com.noura.checkout.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.noura.checkout.domain.enums.CheckoutPaymentMethod;
import com.noura.checkout.dto.checkout.CheckoutPaymentSummaryResponse;
import com.noura.checkout.dto.checkout.CheckoutPlaceOrderRequest;
import com.noura.checkout.exception.CheckoutOperationException;
import com.noura.checkout.integration.model.RemoteApiEnvelope;
import com.noura.checkout.service.PaymentGateway;
import com.noura.checkout.service.model.CheckoutRequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Payment gateway backed by payment-service create and confirm APIs.
 */
@Slf4j
@Component
public class PaymentServiceGateway implements PaymentGateway {

    private static final String HEADER_SUBJECT = "X-Auth-Subject";
    private static final String HEADER_CORRELATION = "X-Correlation-ID";

    private static final ParameterizedTypeReference<RemoteApiEnvelope<PaymentPayload>> PAYMENT_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;

    /**
     * Creates payment-service REST adapter.
     *
     * @param builder rest client builder
     * @param baseUrl payment-service base URL
     */
    public PaymentServiceGateway(
            RestClient.Builder builder,
            @Value("${services.payment.base-url:http://localhost:8092}") String baseUrl
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CheckoutPaymentSummaryResponse createAndConfirmPayment(
            CheckoutRequestContext context,
            CheckoutPlaceOrderRequest request,
            UUID orderId,
            String currencyCode,
            BigDecimal totalAmount,
            String idempotencyKey
    ) {
        PaymentPayload created = createPaymentIntent(context, request, orderId, currencyCode, idempotencyKey);
        PaymentPayload confirmed = confirmPayment(context, created.id(), resolvedAutoCapture(request) ? "CAPTURE" : "AUTHORIZE");
        return new CheckoutPaymentSummaryResponse(
                confirmed.id(),
                confirmed.paymentReference(),
                confirmed.providerCode(),
                confirmed.methodType(),
                confirmed.status(),
                confirmed.amount() == null ? totalAmount : confirmed.amount(),
                confirmed.currencyCode() == null ? currencyCode : confirmed.currencyCode(),
                confirmed.confirmedAt() == null ? Instant.now() : confirmed.confirmedAt()
        );
    }

    /**
     * Creates one payment intent in payment-service.
     *
     * @param context checkout request context
     * @param request checkout request
     * @param orderId created order identifier
     * @param currencyCode order currency code
     * @param idempotencyKey optional checkout idempotency key
     * @return created payment payload
     */
    private PaymentPayload createPaymentIntent(
            CheckoutRequestContext context,
            CheckoutPlaceOrderRequest request,
            UUID orderId,
            String currencyCode,
            String idempotencyKey
    ) {
        try {
            RemoteApiEnvelope<PaymentPayload> envelope = restClient.post()
                    .uri("/api/v1/payments/intents")
                    .headers(headers -> applyHeaders(context, headers))
                    .body(new CreatePaymentIntentPayload(
                            orderId,
                            resolvedMethodType(request),
                            resolvedProviderCode(request),
                            currencyCode,
                            resolvedAutoCapture(request),
                            resolvePaymentIdempotencyKey(idempotencyKey),
                            buildMetadata(request)
                    ))
                    .retrieve()
                    .body(PAYMENT_RESPONSE_TYPE);
            return requirePayment(envelope, "PAYMENT_SERVICE_INVALID_RESPONSE", "Payment service returned an invalid create-intent response");
        } catch (RestClientResponseException ex) {
            log.warn("Payment create-intent failed: status={} body={}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new CheckoutOperationException(
                    HttpStatus.BAD_GATEWAY,
                    "PAYMENT_SERVICE_ERROR",
                    "Payment creation is temporarily unavailable"
            );
        } catch (ResourceAccessException ex) {
            log.warn("Payment service unreachable during create-intent: {}", ex.getMessage());
            throw new CheckoutOperationException(
                    HttpStatus.BAD_GATEWAY,
                    "PAYMENT_SERVICE_UNREACHABLE",
                    "Payment creation is temporarily unavailable"
            );
        }
    }

    /**
     * Confirms the newly created payment synchronously.
     *
     * @param context checkout request context
     * @param paymentId payment identifier
     * @param action confirmation action
     * @return confirmed payment payload
     */
    private PaymentPayload confirmPayment(CheckoutRequestContext context, UUID paymentId, String action) {
        try {
            RemoteApiEnvelope<PaymentPayload> envelope = restClient.post()
                    .uri("/api/v1/payments/{paymentId}/confirm", paymentId)
                    .headers(headers -> applyHeaders(context, headers))
                    .body(new ConfirmPaymentPayload(action, null, null))
                    .retrieve()
                    .body(PAYMENT_RESPONSE_TYPE);
            return requirePayment(envelope, "PAYMENT_SERVICE_INVALID_RESPONSE", "Payment service returned an invalid confirm response");
        } catch (RestClientResponseException ex) {
            log.warn("Payment confirm failed: status={} body={}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new CheckoutOperationException(
                    HttpStatus.BAD_GATEWAY,
                    "PAYMENT_CONFIRMATION_FAILED",
                    "Payment confirmation is temporarily unavailable"
            );
        } catch (ResourceAccessException ex) {
            log.warn("Payment service unreachable during confirm: {}", ex.getMessage());
            throw new CheckoutOperationException(
                    HttpStatus.BAD_GATEWAY,
                    "PAYMENT_SERVICE_UNREACHABLE",
                    "Payment confirmation is temporarily unavailable"
            );
        }
    }

    /**
     * Applies shared actor and tracing headers to payment-service calls.
     *
     * @param context checkout request context
     * @param headers mutable headers
     */
    private void applyHeaders(CheckoutRequestContext context, HttpHeaders headers) {
        if (context.subject() != null && !context.subject().isBlank()) {
            headers.set(HEADER_SUBJECT, context.subject());
        }
        if (context.authorizationHeader() != null && !context.authorizationHeader().isBlank()) {
            headers.set(HttpHeaders.AUTHORIZATION, context.authorizationHeader());
        }
        if (context.correlationId() != null && !context.correlationId().isBlank()) {
            headers.set(HEADER_CORRELATION, context.correlationId());
        }
    }

    /**
     * Validates a remote payment-service envelope.
     *
     * @param envelope remote API envelope
     * @param code stable error code
     * @param detail error detail
     * @return contained payment payload
     */
    private PaymentPayload requirePayment(
            RemoteApiEnvelope<PaymentPayload> envelope,
            String code,
            String detail
    ) {
        if (envelope == null || !Boolean.TRUE.equals(envelope.success()) || envelope.data() == null) {
            throw new CheckoutOperationException(HttpStatus.BAD_GATEWAY, code, detail);
        }
        return envelope.data();
    }

    /**
     * Resolves provider code with startup-safe default.
     *
     * @param request checkout request
     * @return provider code
     */
    private String resolvedProviderCode(CheckoutPlaceOrderRequest request) {
        if (request == null || request.paymentProvider() == null || request.paymentProvider().isBlank()) {
            return "mock";
        }
        return request.paymentProvider().trim();
    }

    /**
     * Resolves checkout payment method to payment-service method type.
     *
     * @param request checkout request
     * @return payment-service method type
     */
    private String resolvedMethodType(CheckoutPlaceOrderRequest request) {
        CheckoutPaymentMethod method = request == null || request.paymentMethod() == null
                ? CheckoutPaymentMethod.CREDIT_CARD
                : request.paymentMethod();
        return switch (method) {
            case CREDIT_CARD -> "CARD";
            case CASH_ON_DELIVERY -> "CASH_ON_DELIVERY";
            case BANK_TRANSFER -> "BANK_TRANSFER";
            case WALLET -> "WALLET";
        };
    }

    /**
     * Resolves auto-capture preference with a synchronous-flow default.
     *
     * @param request checkout request
     * @return auto-capture flag
     */
    private boolean resolvedAutoCapture(CheckoutPlaceOrderRequest request) {
        return request == null || request.paymentAutoCapture() == null || request.paymentAutoCapture();
    }

    /**
     * Derives a stable payment idempotency key from checkout idempotency.
     *
     * @param checkoutIdempotencyKey checkout idempotency key
     * @return payment-scoped idempotency key or {@code null}
     */
    private String resolvePaymentIdempotencyKey(String checkoutIdempotencyKey) {
        if (checkoutIdempotencyKey == null || checkoutIdempotencyKey.isBlank()) {
            return null;
        }
        return checkoutIdempotencyKey.trim() + ":payment";
    }

    /**
     * Builds payment metadata from checkout request fields.
     *
     * @param request checkout request
     * @return metadata map
     */
    private Map<String, Object> buildMetadata(CheckoutPlaceOrderRequest request) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", "checkout-service");
        if (request != null && request.paymentProviderReference() != null && !request.paymentProviderReference().isBlank()) {
            metadata.put("providerReference", request.paymentProviderReference().trim());
        }
        if (request != null && request.couponCode() != null && !request.couponCode().isBlank()) {
            metadata.put("couponCode", request.couponCode().trim());
        }
        return metadata;
    }

    /**
     * Payment create-intent payload accepted by payment-service.
     *
     * @param orderId order identifier
     * @param methodType payment method type
     * @param providerCode provider code
     * @param currencyCode currency code
     * @param autoCapture auto-capture flag
     * @param idempotencyKey idempotency key
     * @param metadata metadata payload
     */
    private record CreatePaymentIntentPayload(
            UUID orderId,
            String methodType,
            String providerCode,
            String currencyCode,
            Boolean autoCapture,
            String idempotencyKey,
            Map<String, Object> metadata
    ) {
    }

    /**
     * Payment confirm payload accepted by payment-service.
     *
     * @param action confirmation action
     * @param providerTransactionId optional transaction ID override
     * @param providerReference optional provider reference
     */
    private record ConfirmPaymentPayload(
            String action,
            String providerTransactionId,
            String providerReference
    ) {
    }

    /**
     * Minimal payment payload returned by payment-service.
     *
     * @param id payment identifier
     * @param paymentReference internal payment reference
     * @param providerCode provider code
     * @param methodType payment method type
     * @param status payment status
     * @param amount amount
     * @param currencyCode currency code
     * @param confirmedAt confirmation timestamp
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PaymentPayload(
            UUID id,
            String paymentReference,
            String providerCode,
            String methodType,
            String status,
            BigDecimal amount,
            String currencyCode,
            Instant confirmedAt
    ) {
    }
}
