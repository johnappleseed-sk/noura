package com.noura.payment.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.payment.config.SandboxProviderProperties;
import com.noura.payment.domain.enums.PaymentAuthorizationStatus;
import com.noura.payment.domain.enums.PaymentCaptureStatus;
import com.noura.payment.domain.enums.PaymentStatus;
import com.noura.payment.exception.PaymentOperationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

/**
 * Deterministic sandbox/mock adapter used until real providers are integrated.
 */
@Component
@RequiredArgsConstructor
public class SandboxPaymentProvider implements PaymentProvider {

    private static final String PROVIDER_CODE = "mock";
    private static final String SANDBOX_ALIAS = "sandbox";
    private static final String SIGNATURE_HEADER = "X-Mock-Signature";

    private final ObjectMapper objectMapper;
    private final SandboxProviderProperties sandboxProviderProperties;

    /**
     * {@inheritDoc}
     */
    @Override
    public String providerCode() {
        return PROVIDER_CODE;
    }

    /**
     * Accepts both {@code mock} and {@code sandbox} aliases.
     *
     * @param requestedProviderCode requested provider code
     * @return {@code true} when this provider can handle the code
     */
    @Override
    public boolean supports(String requestedProviderCode) {
        if (requestedProviderCode == null || requestedProviderCode.isBlank()) {
            return false;
        }
        String normalized = requestedProviderCode.trim().toLowerCase(Locale.ROOT);
        return PROVIDER_CODE.equals(normalized) || SANDBOX_ALIAS.equals(normalized);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProviderOperationResult createPaymentIntent(CreateRequest request) {
        String scenario = resolveScenario(request.metadata());
        if ("fail_create".equals(scenario)) {
            return new ProviderOperationResult(
                    PaymentStatus.FAILED,
                    PaymentAuthorizationStatus.FAILED,
                    PaymentCaptureStatus.FAILED,
                    deterministicTransactionId(request.paymentReference()),
                    "Sandbox create failure triggered by metadata"
            );
        }
        return new ProviderOperationResult(
                PaymentStatus.REQUIRES_CONFIRMATION,
                PaymentAuthorizationStatus.NOT_REQUESTED,
                PaymentCaptureStatus.NOT_CAPTURED,
                deterministicTransactionId(request.paymentReference()),
                null
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProviderOperationResult authorizePayment(AuthorizeRequest request) {
        String scenario = resolveScenario(request.metadata());
        if ("fail_authorize".equals(scenario)) {
            return new ProviderOperationResult(
                    PaymentStatus.FAILED,
                    PaymentAuthorizationStatus.FAILED,
                    PaymentCaptureStatus.NOT_CAPTURED,
                    resolvedTransactionId(request.providerTransactionId(), request.paymentReference()),
                    "Sandbox authorization failure triggered by metadata"
            );
        }
        if ("pending_authorize".equals(scenario)) {
            return new ProviderOperationResult(
                    PaymentStatus.PENDING,
                    PaymentAuthorizationStatus.PENDING,
                    PaymentCaptureStatus.NOT_CAPTURED,
                    resolvedTransactionId(request.providerTransactionId(), request.paymentReference()),
                    null
            );
        }
        return new ProviderOperationResult(
                PaymentStatus.AUTHORIZED,
                PaymentAuthorizationStatus.AUTHORIZED,
                PaymentCaptureStatus.NOT_CAPTURED,
                resolvedTransactionId(request.providerTransactionId(), request.paymentReference()),
                null
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProviderOperationResult capturePayment(CaptureRequest request) {
        String scenario = resolveScenario(request.metadata());
        if ("fail_capture".equals(scenario)) {
            return new ProviderOperationResult(
                    PaymentStatus.FAILED,
                    PaymentAuthorizationStatus.AUTHORIZED,
                    PaymentCaptureStatus.FAILED,
                    resolvedTransactionId(request.providerTransactionId(), request.paymentReference()),
                    "Sandbox capture failure triggered by metadata"
            );
        }
        if ("pending_capture".equals(scenario)) {
            return new ProviderOperationResult(
                    PaymentStatus.PENDING,
                    PaymentAuthorizationStatus.AUTHORIZED,
                    PaymentCaptureStatus.PENDING,
                    resolvedTransactionId(request.providerTransactionId(), request.paymentReference()),
                    null
            );
        }
        return new ProviderOperationResult(
                PaymentStatus.CAPTURED,
                PaymentAuthorizationStatus.AUTHORIZED,
                PaymentCaptureStatus.CAPTURED,
                resolvedTransactionId(request.providerTransactionId(), request.paymentReference()),
                null
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProviderOperationResult fetchPaymentStatus(StatusRequest request) {
        String scenario = resolveScenario(request.metadata());
        if ("fail_authorize".equals(scenario) || "fail_capture".equals(scenario) || "fail_create".equals(scenario)) {
            return new ProviderOperationResult(
                    PaymentStatus.FAILED,
                    request.currentStatus() == PaymentStatus.AUTHORIZED
                            ? PaymentAuthorizationStatus.AUTHORIZED
                            : PaymentAuthorizationStatus.FAILED,
                    request.currentStatus() == PaymentStatus.AUTHORIZED
                            ? PaymentCaptureStatus.FAILED
                            : PaymentCaptureStatus.NOT_CAPTURED,
                    resolvedTransactionId(request.providerTransactionId(), request.paymentReference()),
                    "Sandbox provider reported a deterministic failure"
            );
        }
        if ("pending_authorize".equals(scenario)) {
            return new ProviderOperationResult(
                    PaymentStatus.PENDING,
                    PaymentAuthorizationStatus.PENDING,
                    PaymentCaptureStatus.NOT_CAPTURED,
                    resolvedTransactionId(request.providerTransactionId(), request.paymentReference()),
                    null
            );
        }
        if ("pending_capture".equals(scenario)) {
            return new ProviderOperationResult(
                    PaymentStatus.PENDING,
                    PaymentAuthorizationStatus.AUTHORIZED,
                    PaymentCaptureStatus.PENDING,
                    resolvedTransactionId(request.providerTransactionId(), request.paymentReference()),
                    null
            );
        }
        if (request.currentStatus() == PaymentStatus.AUTHORIZED) {
            return new ProviderOperationResult(
                    PaymentStatus.AUTHORIZED,
                    PaymentAuthorizationStatus.AUTHORIZED,
                    PaymentCaptureStatus.NOT_CAPTURED,
                    resolvedTransactionId(request.providerTransactionId(), request.paymentReference()),
                    null
            );
        }
        return new ProviderOperationResult(
                PaymentStatus.CAPTURED,
                PaymentAuthorizationStatus.AUTHORIZED,
                PaymentCaptureStatus.CAPTURED,
                resolvedTransactionId(request.providerTransactionId(), request.paymentReference()),
                null
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProviderWebhookNotification parseWebhook(WebhookRequest request) {
        verifySignatureIfConfigured(request.headers());
        try {
            JsonNode root = objectMapper.readTree(request.payload());
            String eventId = resolveText(root, "eventId");
            String eventType = resolveText(root, "eventType");
            String paymentReference = resolveText(root, "paymentReference");
            String providerTransactionId = resolveText(root, "providerTransactionId");
            String statusValue = resolveText(root, "status");
            String failureReason = resolveText(root, "failureReason");

            PaymentStatus status = resolveWebhookStatus(eventType, statusValue);
            if (paymentReference == null && providerTransactionId == null) {
                throw new PaymentOperationException(
                        HttpStatus.BAD_REQUEST,
                        "PAYMENT_WEBHOOK_REFERENCE_REQUIRED",
                        "Webhook payload must include paymentReference or providerTransactionId"
                );
            }

            return new ProviderWebhookNotification(
                    providerCode(),
                    eventId != null ? eventId : fallbackEventId(request.payload()),
                    eventType != null ? eventType : "payment." + status.name().toLowerCase(Locale.ROOT),
                    paymentReference,
                    providerTransactionId,
                    status,
                    deriveAuthorizationStatus(status),
                    deriveCaptureStatus(status),
                    resolveFailureReason(status, failureReason),
                    sandboxProviderProperties.getWebhookSecret() != null
                            && !sandboxProviderProperties.getWebhookSecret().isBlank(),
                    request.payload()
            );
        } catch (PaymentOperationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PaymentOperationException(
                    HttpStatus.BAD_REQUEST,
                    "PAYMENT_WEBHOOK_INVALID_PAYLOAD",
                    "Webhook payload is not valid JSON for the sandbox provider"
            );
        }
    }

    /**
     * Resolves a deterministic sandbox scenario from payment metadata.
     *
     * @param metadata payment metadata
     * @return normalized scenario code or {@code null}
     */
    private String resolveScenario(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        Object scenario = metadata.get("sandboxScenario");
        if (scenario == null) {
            return null;
        }
        String normalized = String.valueOf(scenario).trim();
        return normalized.isEmpty() ? null : normalized.toLowerCase(Locale.ROOT);
    }

    /**
     * Returns a deterministic provider transaction ID.
     *
     * @param paymentReference internal payment reference
     * @return provider transaction identifier
     */
    private String deterministicTransactionId(String paymentReference) {
        String normalized = paymentReference == null ? "unknown" : paymentReference.replaceAll("[^A-Za-z0-9]", "");
        if (normalized.length() > 18) {
            normalized = normalized.substring(normalized.length() - 18);
        }
        return "mock_txn_" + normalized.toLowerCase(Locale.ROOT);
    }

    /**
     * Reuses the provider transaction ID when one already exists.
     *
     * @param providerTransactionId existing provider transaction ID
     * @param paymentReference payment reference fallback
     * @return resolved provider transaction ID
     */
    private String resolvedTransactionId(String providerTransactionId, String paymentReference) {
        if (providerTransactionId != null && !providerTransactionId.isBlank()) {
            return providerTransactionId.trim();
        }
        return deterministicTransactionId(paymentReference);
    }

    /**
     * Verifies placeholder webhook signature when a sandbox secret is configured.
     *
     * @param headers inbound headers
     */
    private void verifySignatureIfConfigured(Map<String, String> headers) {
        String expectedSecret = normalizeNullable(sandboxProviderProperties.getWebhookSecret());
        if (expectedSecret == null) {
            return;
        }
        String providedSignature = normalizeNullable(headers == null ? null : headers.get(SIGNATURE_HEADER));
        if (!expectedSecret.equals(providedSignature)) {
            throw new PaymentOperationException(
                    HttpStatus.BAD_REQUEST,
                    "PAYMENT_WEBHOOK_SIGNATURE_INVALID",
                    "Invalid sandbox webhook signature"
            );
        }
    }

    /**
     * Resolves a normalized payment status from webhook fields.
     *
     * @param eventType event type label
     * @param statusValue explicit status field
     * @return resolved payment status
     */
    private PaymentStatus resolveWebhookStatus(String eventType, String statusValue) {
        if (statusValue != null) {
            try {
                return PaymentStatus.valueOf(statusValue.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                throw new PaymentOperationException(
                        HttpStatus.BAD_REQUEST,
                        "PAYMENT_WEBHOOK_STATUS_INVALID",
                        "Webhook status is not recognized"
                );
            }
        }
        if (eventType == null) {
            throw new PaymentOperationException(
                    HttpStatus.BAD_REQUEST,
                    "PAYMENT_WEBHOOK_EVENT_TYPE_REQUIRED",
                    "Webhook payload must include eventType or status"
            );
        }
        return switch (eventType.trim().toLowerCase(Locale.ROOT)) {
            case "payment.authorized" -> PaymentStatus.AUTHORIZED;
            case "payment.captured", "payment.succeeded" -> PaymentStatus.CAPTURED;
            case "payment.failed" -> PaymentStatus.FAILED;
            case "payment.canceled", "payment.cancelled" -> PaymentStatus.CANCELED;
            case "payment.refunded" -> PaymentStatus.REFUNDED;
            case "payment.pending" -> PaymentStatus.PENDING;
            default -> throw new PaymentOperationException(
                    HttpStatus.BAD_REQUEST,
                    "PAYMENT_WEBHOOK_EVENT_TYPE_INVALID",
                    "Webhook eventType is not supported by the sandbox provider"
            );
        };
    }

    /**
     * Derives authorization status from normalized payment status.
     *
     * @param status normalized payment status
     * @return authorization status
     */
    private PaymentAuthorizationStatus deriveAuthorizationStatus(PaymentStatus status) {
        return switch (status) {
            case CREATED, REQUIRES_CONFIRMATION -> PaymentAuthorizationStatus.NOT_REQUESTED;
            case PENDING -> PaymentAuthorizationStatus.PENDING;
            case AUTHORIZED, CAPTURED, REFUNDED -> PaymentAuthorizationStatus.AUTHORIZED;
            case FAILED -> PaymentAuthorizationStatus.FAILED;
            case CANCELED -> PaymentAuthorizationStatus.CANCELED;
        };
    }

    /**
     * Derives capture status from normalized payment status.
     *
     * @param status normalized payment status
     * @return capture status
     */
    private PaymentCaptureStatus deriveCaptureStatus(PaymentStatus status) {
        return switch (status) {
            case CREATED, REQUIRES_CONFIRMATION, AUTHORIZED -> PaymentCaptureStatus.NOT_CAPTURED;
            case PENDING -> PaymentCaptureStatus.PENDING;
            case CAPTURED -> PaymentCaptureStatus.CAPTURED;
            case FAILED -> PaymentCaptureStatus.FAILED;
            case CANCELED -> PaymentCaptureStatus.CANCELED;
            case REFUNDED -> PaymentCaptureStatus.REFUNDED;
        };
    }

    /**
     * Resolves a failure reason when one is required.
     *
     * @param status normalized payment status
     * @param failureReason failure reason from payload
     * @return resolved failure reason
     */
    private String resolveFailureReason(PaymentStatus status, String failureReason) {
        if (status == PaymentStatus.FAILED) {
            return failureReason == null || failureReason.isBlank()
                    ? "Sandbox payment failure"
                    : failureReason.trim();
        }
        return normalizeNullable(failureReason);
    }

    /**
     * Resolves one optional JSON text field.
     *
     * @param root source JSON node
     * @param fieldName field name
     * @return normalized text or {@code null}
     */
    private String resolveText(JsonNode root, String fieldName) {
        if (root == null || fieldName == null) {
            return null;
        }
        JsonNode node = root.get(fieldName);
        return node == null || node.isNull() ? null : normalizeNullable(node.asText());
    }

    /**
     * Builds a stable fallback event ID from raw payload content.
     *
     * @param payload raw payload
     * @return derived event identifier
     */
    private String fallbackEventId(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return "mock_evt_" + HexFormat.of().formatHex(hash).substring(0, 24);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }

    /**
     * Trims text and normalizes blanks to {@code null}.
     *
     * @param value source text
     * @return normalized text
     */
    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
