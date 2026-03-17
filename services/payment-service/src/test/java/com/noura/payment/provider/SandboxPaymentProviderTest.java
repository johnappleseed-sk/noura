package com.noura.payment.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.payment.config.SandboxProviderProperties;
import com.noura.payment.domain.enums.PaymentAuthorizationStatus;
import com.noura.payment.domain.enums.PaymentCaptureStatus;
import com.noura.payment.domain.enums.PaymentStatus;
import com.noura.payment.exception.PaymentOperationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Unit tests for {@link SandboxPaymentProvider}.
 */
class SandboxPaymentProviderTest {

    /**
     * Verifies authorize flow returns deterministic authorization success by default.
     */
    @Test
    void shouldAuthorizePaymentByDefault() {
        SandboxPaymentProvider provider = new SandboxPaymentProvider(new ObjectMapper(), new SandboxProviderProperties());

        PaymentProvider.ProviderOperationResult result = provider.authorizePayment(
                new PaymentProvider.AuthorizeRequest(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "pay_123",
                        null,
                        new BigDecimal("19.9900"),
                        "USD",
                        Map.of()
                )
        );

        Assertions.assertEquals(PaymentStatus.AUTHORIZED, result.status());
        Assertions.assertEquals(PaymentAuthorizationStatus.AUTHORIZED, result.authorizationStatus());
        Assertions.assertEquals(PaymentCaptureStatus.NOT_CAPTURED, result.captureStatus());
        Assertions.assertTrue(result.providerTransactionId().startsWith("mock_txn_"));
    }

    /**
     * Verifies capture flow can simulate a pending provider settlement.
     */
    @Test
    void shouldReturnPendingCaptureForScenario() {
        SandboxPaymentProvider provider = new SandboxPaymentProvider(new ObjectMapper(), new SandboxProviderProperties());

        PaymentProvider.ProviderOperationResult result = provider.capturePayment(
                new PaymentProvider.CaptureRequest(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "pay_456",
                        "mock_txn_pay_456",
                        new BigDecimal("42.0000"),
                        "USD",
                        Map.of("sandboxScenario", "pending_capture")
                )
        );

        Assertions.assertEquals(PaymentStatus.PENDING, result.status());
        Assertions.assertEquals(PaymentAuthorizationStatus.AUTHORIZED, result.authorizationStatus());
        Assertions.assertEquals(PaymentCaptureStatus.PENDING, result.captureStatus());
    }

    /**
     * Verifies webhook payload parsing enforces the configured placeholder signature.
     */
    @Test
    void shouldRequireSignatureWhenConfigured() {
        SandboxProviderProperties properties = new SandboxProviderProperties();
        properties.setWebhookSecret("sandbox-secret");
        SandboxPaymentProvider provider = new SandboxPaymentProvider(new ObjectMapper(), properties);

        PaymentOperationException exception = Assertions.assertThrows(
                PaymentOperationException.class,
                () -> provider.parseWebhook(
                        new PaymentProvider.WebhookRequest(
                                "mock",
                                "{\"eventId\":\"evt-1\",\"eventType\":\"payment.captured\",\"paymentReference\":\"pay_123\"}",
                                Map.of()
                        )
                )
        );

        Assertions.assertEquals("PAYMENT_WEBHOOK_SIGNATURE_INVALID", exception.getCode());
    }

    /**
     * Verifies webhook payload parsing normalizes provider events into internal status vocabulary.
     */
    @Test
    void shouldParseWebhookPayload() {
        SandboxPaymentProvider provider = new SandboxPaymentProvider(new ObjectMapper(), new SandboxProviderProperties());

        PaymentProvider.ProviderWebhookNotification notification = provider.parseWebhook(
                new PaymentProvider.WebhookRequest(
                        "mock",
                        "{\"eventId\":\"evt-2\",\"eventType\":\"payment.captured\",\"paymentReference\":\"pay_999\",\"providerTransactionId\":\"mock_txn_pay_999\"}",
                        Map.of()
                )
        );

        Assertions.assertEquals("evt-2", notification.providerEventId());
        Assertions.assertEquals(PaymentStatus.CAPTURED, notification.status());
        Assertions.assertEquals(PaymentAuthorizationStatus.AUTHORIZED, notification.authorizationStatus());
        Assertions.assertEquals(PaymentCaptureStatus.CAPTURED, notification.captureStatus());
    }
}
