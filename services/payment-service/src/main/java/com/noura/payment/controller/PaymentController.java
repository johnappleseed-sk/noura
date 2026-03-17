package com.noura.payment.controller;

import com.noura.payment.common.ApiResponse;
import com.noura.payment.config.InternalApiProperties;
import com.noura.payment.controller.support.PaymentRequestContextResolver;
import com.noura.payment.dto.payment.ConfirmPaymentRequest;
import com.noura.payment.dto.payment.CreatePaymentIntentRequest;
import com.noura.payment.dto.payment.PaymentResponse;
import com.noura.payment.dto.payment.PaymentStatusUpdateRequest;
import com.noura.payment.dto.payment.PaymentWebhookResponse;
import com.noura.payment.exception.PaymentOperationException;
import com.noura.payment.service.PaymentService;
import com.noura.payment.service.model.PaymentRequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for payment intent creation, confirmation, lookup, and webhook ingestion.
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentRequestContextResolver contextResolver;
    private final InternalApiProperties internalApiProperties;

    /**
     * Creates one payment intent for an existing order.
     *
     * @param requestBody create-intent payload
     * @param request current HTTP request
     * @return payment response envelope
     */
    @PostMapping({"/api/v1/payments/intents", "/api/payments/intents"})
    public ApiResponse<PaymentResponse> createPaymentIntent(
            @Valid @RequestBody CreatePaymentIntentRequest requestBody,
            HttpServletRequest request
    ) {
        PaymentRequestContext context = contextResolver.resolve(request);
        PaymentResponse data = paymentService.createPaymentIntent(context, requestBody);
        return ApiResponse.ok("Payment intent created", data, request.getRequestURI());
    }

    /**
     * Confirms one payment intent using authorize or capture semantics.
     *
     * @param paymentId payment identifier
     * @param requestBody confirm payload
     * @param request current HTTP request
     * @return payment response envelope
     */
    @PostMapping({"/api/v1/payments/{paymentId}/confirm", "/api/payments/{paymentId}/confirm"})
    public ApiResponse<PaymentResponse> confirmPayment(
            @PathVariable UUID paymentId,
            @Valid @RequestBody(required = false) ConfirmPaymentRequest requestBody,
            HttpServletRequest request
    ) {
        PaymentRequestContext context = contextResolver.resolve(request);
        PaymentResponse data = paymentService.confirmPayment(
                context,
                paymentId,
                requestBody == null ? new ConfirmPaymentRequest(null, null, null) : requestBody
        );
        return ApiResponse.ok("Payment confirmed", data, request.getRequestURI());
    }

    /**
     * Retrieves one payment by identifier.
     *
     * @param paymentId payment identifier
     * @param request current HTTP request
     * @return payment response envelope
     */
    @GetMapping({"/api/v1/payments/{paymentId}", "/api/payments/{paymentId}"})
    public ApiResponse<PaymentResponse> getPaymentById(
            @PathVariable UUID paymentId,
            HttpServletRequest request
    ) {
        PaymentRequestContext context = contextResolver.resolve(request);
        PaymentResponse data = paymentService.getPaymentById(context, paymentId);
        return ApiResponse.ok("Payment", data, request.getRequestURI());
    }

    /**
     * Retrieves the latest payment for one order.
     *
     * @param orderId order identifier
     * @param request current HTTP request
     * @return payment response envelope
     */
    @GetMapping({"/api/v1/payments/order/{orderId}", "/api/payments/order/{orderId}"})
    public ApiResponse<PaymentResponse> getPaymentByOrderId(
            @PathVariable UUID orderId,
            HttpServletRequest request
    ) {
        PaymentRequestContext context = contextResolver.resolve(request);
        PaymentResponse data = paymentService.getLatestPaymentByOrderId(context, orderId);
        return ApiResponse.ok("Payment", data, request.getRequestURI());
    }

    /**
     * Applies one trusted internal payment status update.
     *
     * @param requestBody status update payload
     * @param providedApiKey optional internal API key
     * @param request current HTTP request
     * @return payment response envelope
     */
    @PostMapping("/internal/payments/status-update")
    public ApiResponse<PaymentResponse> updatePaymentStatus(
            @Valid @RequestBody PaymentStatusUpdateRequest requestBody,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String providedApiKey,
            HttpServletRequest request
    ) {
        validateInternalApiKey(providedApiKey);
        PaymentResponse data = paymentService.updatePaymentStatus(requestBody, "internal");
        return ApiResponse.ok("Payment status updated", data, request.getRequestURI());
    }

    /**
     * Handles webhook-ready provider deliveries.
     *
     * @param providerCode provider code
     * @param payload raw payload
     * @param request current HTTP request
     * @return accepted webhook response envelope
     */
    @PostMapping({"/api/v1/payments/webhooks/{providerCode}", "/api/payments/webhooks/{providerCode}"})
    public ResponseEntity<ApiResponse<PaymentWebhookResponse>> handleWebhook(
            @PathVariable String providerCode,
            @RequestBody String payload,
            HttpServletRequest request
    ) {
        PaymentWebhookResponse data = paymentService.handleWebhook(providerCode, payload, collectHeaders(request));
        return ResponseEntity.accepted().body(ApiResponse.ok("Webhook accepted", data, request.getRequestURI()));
    }

    /**
     * Validates internal API key when one is configured.
     *
     * @param providedApiKey API key from request header
     */
    private void validateInternalApiKey(String providedApiKey) {
        String configuredApiKey = trimToNull(internalApiProperties.getApiKey());
        if (configuredApiKey == null) {
            return;
        }
        if (!configuredApiKey.equals(trimToNull(providedApiKey))) {
            throw new PaymentOperationException(
                    HttpStatus.FORBIDDEN,
                    "INTERNAL_API_KEY_INVALID",
                    "Invalid internal API key"
            );
        }
    }

    /**
     * Collects request headers into a simple string map for provider parsing.
     *
     * @param request current request
     * @return normalized header map
     */
    private Map<String, String> collectHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        request.getHeaderNames().asIterator().forEachRemaining(name -> headers.put(name, request.getHeader(name)));
        return headers;
    }

    /**
     * Trims text and normalizes blanks to {@code null}.
     *
     * @param value source text
     * @return normalized text
     */
    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
