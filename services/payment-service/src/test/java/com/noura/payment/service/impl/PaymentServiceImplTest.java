package com.noura.payment.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.payment.domain.entity.PaymentTransaction;
import com.noura.payment.domain.entity.PaymentWebhookEvent;
import com.noura.payment.domain.enums.PaymentAuthorizationStatus;
import com.noura.payment.domain.enums.PaymentCaptureStatus;
import com.noura.payment.domain.enums.PaymentConfirmationAction;
import com.noura.payment.domain.enums.PaymentMethodType;
import com.noura.payment.domain.enums.PaymentStatus;
import com.noura.payment.domain.enums.PaymentWebhookProcessingStatus;
import com.noura.payment.dto.payment.ConfirmPaymentRequest;
import com.noura.payment.dto.payment.CreatePaymentIntentRequest;
import com.noura.payment.dto.payment.PaymentResponse;
import com.noura.payment.dto.payment.PaymentStatusUpdateRequest;
import com.noura.payment.dto.payment.PaymentWebhookResponse;
import com.noura.payment.exception.PaymentOperationException;
import com.noura.payment.integration.client.OrderServiceClient;
import com.noura.payment.provider.PaymentProvider;
import com.noura.payment.provider.PaymentProviderRegistry;
import com.noura.payment.repository.PaymentTransactionRepository;
import com.noura.payment.repository.PaymentWebhookEventRepository;
import com.noura.payment.service.model.PaymentRequestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PaymentServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private PaymentWebhookEventRepository paymentWebhookEventRepository;

    @Mock
    private OrderServiceClient orderServiceClient;

    @Mock
    private PaymentProviderRegistry paymentProviderRegistry;

    @Mock
    private PaymentProvider paymentProvider;

    private PaymentServiceImpl paymentService;

    /**
     * Initializes service under test before each test case.
     */
    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(
                paymentTransactionRepository,
                paymentWebhookEventRepository,
                orderServiceClient,
                paymentProviderRegistry,
                new ObjectMapper()
        );
    }

    /**
     * Verifies create-intent retries reuse the existing idempotent payment instead of calling the provider again.
     */
    @Test
    void shouldReturnExistingPaymentWhenIdempotencyKeyMatches() {
        UUID orderId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        PaymentTransaction existing = payment(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                orderId,
                "customer-1",
                PaymentStatus.REQUIRES_CONFIRMATION
        );
        existing.setIdempotencyKey("idem-1");

        when(orderServiceClient.getOrderById(any(), any(), eq(orderId))).thenReturn(order(orderId, "customer-1"));
        when(paymentTransactionRepository.findByOrderIdAndCustomerRefAndIdempotencyKey(orderId, "customer-1", "idem-1"))
                .thenReturn(Optional.of(existing));

        PaymentResponse response = paymentService.createPaymentIntent(
                new PaymentRequestContext("customer-1", null, Set.of(), false),
                new CreatePaymentIntentRequest(orderId, PaymentMethodType.CARD, "mock", "USD", false, "idem-1", Map.of())
        );

        Assertions.assertEquals(existing.getId(), response.id());
        verify(paymentProviderRegistry, never()).resolve(any());
        verify(paymentTransactionRepository, never()).save(any(PaymentTransaction.class));
    }

    /**
     * Verifies create-intent persists provider-backed payment records using order totals, not caller-supplied amounts.
     */
    @Test
    void shouldCreatePaymentIntentFromOrderSnapshot() {
        UUID orderId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        when(orderServiceClient.getOrderById(any(), any(), eq(orderId))).thenReturn(order(orderId, "customer-2"));
        when(paymentTransactionRepository.findByOrderIdAndCustomerRefAndIdempotencyKey(orderId, "customer-2", "idem-2"))
                .thenReturn(Optional.empty());
        when(paymentProviderRegistry.resolve("mock")).thenReturn(paymentProvider);
        when(paymentProvider.providerCode()).thenReturn("mock");
        when(paymentProvider.createPaymentIntent(any())).thenReturn(new PaymentProvider.ProviderOperationResult(
                PaymentStatus.REQUIRES_CONFIRMATION,
                PaymentAuthorizationStatus.NOT_REQUESTED,
                PaymentCaptureStatus.NOT_CAPTURED,
                "mock_txn_created",
                null
        ));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> {
            PaymentTransaction payment = invocation.getArgument(0, PaymentTransaction.class);
            payment.setId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
            payment.setCreatedAt(Instant.now());
            payment.setUpdatedAt(Instant.now());
            return payment;
        });

        PaymentResponse response = paymentService.createPaymentIntent(
                new PaymentRequestContext("customer-2", null, Set.of(), false),
                new CreatePaymentIntentRequest(orderId, PaymentMethodType.CARD, "mock", "USD", false, "idem-2", Map.of("sandboxScenario", "pending_capture"))
        );

        Assertions.assertEquals(orderId, response.orderId());
        Assertions.assertEquals(new BigDecimal("49.9900"), response.amount());
        Assertions.assertEquals("USD", response.currencyCode());
        Assertions.assertEquals(PaymentStatus.REQUIRES_CONFIRMATION, response.status());
        Assertions.assertEquals("mock", response.providerCode());
        Assertions.assertEquals("pending_capture", response.metadata().get("sandboxScenario"));
    }

    /**
     * Verifies invalid terminal-to-nonterminal manual transitions are rejected.
     */
    @Test
    void shouldRejectInvalidManualStatusTransition() {
        UUID paymentId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        PaymentTransaction existing = payment(paymentId, UUID.randomUUID(), "customer-3", PaymentStatus.CAPTURED);
        existing.setAuthorizationStatus(PaymentAuthorizationStatus.AUTHORIZED);
        existing.setCaptureStatus(PaymentCaptureStatus.CAPTURED);

        when(paymentTransactionRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(existing));

        PaymentOperationException exception = Assertions.assertThrows(
                PaymentOperationException.class,
                () -> paymentService.updatePaymentStatus(
                        new PaymentStatusUpdateRequest(paymentId, null, "mock", PaymentStatus.AUTHORIZED, null, null, null, null, null),
                        "internal"
                )
        );

        Assertions.assertEquals("PAYMENT_STATUS_INVALID_TRANSITION", exception.getCode());
        verify(paymentTransactionRepository, never()).save(any(PaymentTransaction.class));
    }

    /**
     * Verifies duplicate webhooks are short-circuited using persisted event identity.
     */
    @Test
    void shouldReturnDuplicateWebhookResponseWhenEventAlreadyExists() {
        PaymentWebhookEvent existingEvent = new PaymentWebhookEvent();
        existingEvent.setProviderCode("mock");
        existingEvent.setProviderEventId("evt-1");
        existingEvent.setEventType("payment.captured");
        existingEvent.setProcessingStatus(PaymentWebhookProcessingStatus.PROCESSED);
        existingEvent.setSignatureVerified(false);

        PaymentTransaction payment = payment(
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                UUID.randomUUID(),
                "customer-4",
                PaymentStatus.CAPTURED
        );
        existingEvent.setPaymentTransaction(payment);

        when(paymentProviderRegistry.resolve("mock")).thenReturn(paymentProvider);
        when(paymentProvider.parseWebhook(any())).thenReturn(new PaymentProvider.ProviderWebhookNotification(
                "mock",
                "evt-1",
                "payment.captured",
                "pay_dup",
                "mock_txn_dup",
                PaymentStatus.CAPTURED,
                PaymentAuthorizationStatus.AUTHORIZED,
                PaymentCaptureStatus.CAPTURED,
                null,
                false,
                "{\"eventId\":\"evt-1\"}"
        ));
        when(paymentWebhookEventRepository.findByProviderCodeAndProviderEventId("mock", "evt-1"))
                .thenReturn(Optional.of(existingEvent));

        PaymentWebhookResponse response = paymentService.handleWebhook(
                "mock",
                "{\"eventId\":\"evt-1\"}",
                Map.of()
        );

        Assertions.assertTrue(response.duplicate());
        Assertions.assertEquals(PaymentWebhookProcessingStatus.PROCESSED, response.processingStatus());
        Assertions.assertEquals(payment.getId(), response.paymentId());
        verify(paymentTransactionRepository, never()).save(any(PaymentTransaction.class));
    }

    /**
     * Verifies new webhook deliveries update payment state and persist delivery metadata.
     */
    @Test
    void shouldProcessWebhookAndUpdatePayment() {
        UUID paymentId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        PaymentTransaction payment = payment(paymentId, UUID.randomUUID(), "customer-5", PaymentStatus.AUTHORIZED);
        payment.setProviderCode("mock");
        payment.setProviderTransactionId("mock_txn_555");
        payment.setAuthorizationStatus(PaymentAuthorizationStatus.AUTHORIZED);
        payment.setCaptureStatus(PaymentCaptureStatus.NOT_CAPTURED);
        payment.setAuthorizedAt(Instant.now());
        payment.setAuthorizedAmount(new BigDecimal("49.9900"));

        when(paymentProviderRegistry.resolve("mock")).thenReturn(paymentProvider);
        when(paymentProvider.parseWebhook(any())).thenReturn(new PaymentProvider.ProviderWebhookNotification(
                "mock",
                "evt-2",
                "payment.captured",
                null,
                "mock_txn_555",
                PaymentStatus.CAPTURED,
                PaymentAuthorizationStatus.AUTHORIZED,
                PaymentCaptureStatus.CAPTURED,
                null,
                false,
                "{\"eventId\":\"evt-2\"}"
        ));
        when(paymentWebhookEventRepository.findByProviderCodeAndProviderEventId("mock", "evt-2"))
                .thenReturn(Optional.empty());
        when(paymentWebhookEventRepository.save(any(PaymentWebhookEvent.class))).thenAnswer(invocation -> invocation.getArgument(0, PaymentWebhookEvent.class));
        when(paymentTransactionRepository.findByProviderCodeAndProviderTransactionIdForUpdate("mock", "mock_txn_555"))
                .thenReturn(Optional.of(payment));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0, PaymentTransaction.class));

        PaymentWebhookResponse response = paymentService.handleWebhook(
                "mock",
                "{\"eventId\":\"evt-2\"}",
                Map.of()
        );

        Assertions.assertFalse(response.duplicate());
        Assertions.assertEquals(PaymentWebhookProcessingStatus.PROCESSED, response.processingStatus());
        Assertions.assertEquals(PaymentStatus.CAPTURED, response.paymentStatus());
        Assertions.assertEquals(PaymentStatus.CAPTURED, payment.getStatus());
        Assertions.assertEquals(PaymentCaptureStatus.CAPTURED, payment.getCaptureStatus());
        Assertions.assertNotNull(payment.getCapturedAt());
    }

    /**
     * Verifies confirm-payment captures an intent and persists the provider-normalized terminal state.
     */
    @Test
    void shouldConfirmPaymentAndPersistCapturedState() {
        UUID paymentId = UUID.fromString("66666666-6666-6666-6666-666666666666");
        PaymentTransaction payment = payment(
                paymentId,
                UUID.fromString("77777777-7777-7777-7777-777777777777"),
                "customer-6",
                PaymentStatus.REQUIRES_CONFIRMATION
        );
        payment.setAuthorizationStatus(PaymentAuthorizationStatus.NOT_REQUESTED);
        payment.setCaptureStatus(PaymentCaptureStatus.NOT_CAPTURED);

        when(paymentTransactionRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(payment));
        when(paymentProviderRegistry.resolve("mock")).thenReturn(paymentProvider);
        when(paymentProvider.capturePayment(any())).thenReturn(new PaymentProvider.ProviderOperationResult(
                PaymentStatus.CAPTURED,
                PaymentAuthorizationStatus.AUTHORIZED,
                PaymentCaptureStatus.CAPTURED,
                "mock_txn_confirmed",
                null
        ));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0, PaymentTransaction.class));

        PaymentResponse response = paymentService.confirmPayment(
                new PaymentRequestContext("customer-6", null, Set.of(), false),
                paymentId,
                new ConfirmPaymentRequest(PaymentConfirmationAction.CAPTURE, null, null)
        );

        Assertions.assertEquals(PaymentStatus.CAPTURED, response.status());
        Assertions.assertEquals(PaymentCaptureStatus.CAPTURED, response.captureStatus());
        Assertions.assertEquals("mock_txn_confirmed", response.providerTransactionId());
        Assertions.assertNotNull(response.confirmedAt());
        Assertions.assertNotNull(response.capturedAt());
        verify(paymentTransactionRepository).save(payment);
    }

    /**
     * Creates a minimal order payload for tests.
     *
     * @param orderId order identifier
     * @param customerRef customer reference
     * @return order payload
     */
    private OrderServiceClient.OrderPayload order(UUID orderId, String customerRef) {
        return new OrderServiceClient.OrderPayload(
                orderId,
                "ORD-20260317-0001",
                customerRef,
                new BigDecimal("49.9900"),
                "USD",
                null,
                "PAYMENT_PENDING"
        );
    }

    /**
     * Creates a minimal payment aggregate for tests.
     *
     * @param paymentId payment identifier
     * @param orderId order identifier
     * @param customerRef customer reference
     * @param status payment status
     * @return payment aggregate
     */
    private PaymentTransaction payment(UUID paymentId, UUID orderId, String customerRef, PaymentStatus status) {
        PaymentTransaction payment = new PaymentTransaction();
        payment.setId(paymentId);
        payment.setOrderId(orderId);
        payment.setCustomerRef(customerRef);
        payment.setPaymentReference("pay_" + paymentId.toString().substring(0, 8));
        payment.setMethodType(PaymentMethodType.CARD);
        payment.setProviderCode("mock");
        payment.setAmount(new BigDecimal("49.9900"));
        payment.setCurrencyCode("USD");
        payment.setStatus(status);
        payment.setCreatedAt(Instant.now());
        payment.setUpdatedAt(Instant.now());
        payment.setRequestedAt(Instant.now());
        return payment;
    }
}
