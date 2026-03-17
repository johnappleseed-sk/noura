package com.noura.payment.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.payment.domain.entity.PaymentTransaction;
import com.noura.payment.domain.entity.PaymentWebhookEvent;
import com.noura.payment.domain.enums.PaymentAuthorizationStatus;
import com.noura.payment.domain.enums.PaymentCaptureStatus;
import com.noura.payment.domain.enums.PaymentConfirmationAction;
import com.noura.payment.domain.enums.PaymentStatus;
import com.noura.payment.domain.enums.PaymentWebhookProcessingStatus;
import com.noura.payment.dto.payment.ConfirmPaymentRequest;
import com.noura.payment.dto.payment.CreatePaymentIntentRequest;
import com.noura.payment.dto.payment.PaymentResponse;
import com.noura.payment.dto.payment.PaymentStatusUpdateRequest;
import com.noura.payment.dto.payment.PaymentWebhookResponse;
import com.noura.payment.exception.NotFoundException;
import com.noura.payment.exception.PaymentOperationException;
import com.noura.payment.integration.client.OrderServiceClient;
import com.noura.payment.provider.PaymentProvider;
import com.noura.payment.provider.PaymentProviderRegistry;
import com.noura.payment.repository.PaymentTransactionRepository;
import com.noura.payment.repository.PaymentWebhookEventRepository;
import com.noura.payment.service.PaymentService;
import com.noura.payment.service.model.PaymentRequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Default implementation of {@link PaymentService}.
 *
 * <p>The service owns internal payment lifecycle state, provider interaction,
 * webhook idempotency, and read access checks. It intentionally does not mutate
 * order lifecycle state in this first extraction.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private static final Map<PaymentStatus, Set<PaymentStatus>> STATUS_FLOW = Map.of(
            PaymentStatus.CREATED, Set.of(PaymentStatus.REQUIRES_CONFIRMATION, PaymentStatus.PENDING, PaymentStatus.FAILED, PaymentStatus.CANCELED),
            PaymentStatus.REQUIRES_CONFIRMATION, Set.of(PaymentStatus.PENDING, PaymentStatus.AUTHORIZED, PaymentStatus.CAPTURED, PaymentStatus.FAILED, PaymentStatus.CANCELED),
            PaymentStatus.PENDING, Set.of(PaymentStatus.AUTHORIZED, PaymentStatus.CAPTURED, PaymentStatus.FAILED, PaymentStatus.CANCELED),
            PaymentStatus.AUTHORIZED, Set.of(PaymentStatus.CAPTURED, PaymentStatus.CANCELED),
            PaymentStatus.CAPTURED, Set.of(PaymentStatus.REFUNDED),
            PaymentStatus.REFUNDED, Set.of(),
            PaymentStatus.FAILED, Set.of(),
            PaymentStatus.CANCELED, Set.of()
    );

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentWebhookEventRepository paymentWebhookEventRepository;
    private final OrderServiceClient orderServiceClient;
    private final PaymentProviderRegistry paymentProviderRegistry;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public PaymentResponse createPaymentIntent(PaymentRequestContext context, CreatePaymentIntentRequest request) {
        OrderServiceClient.OrderPayload order = orderServiceClient.getOrderById(context, currentCorrelationId(), request.orderId());
        assertOrderAccess(context, order);

        String customerRef = normalizeNullable(order.customerRef());
        if (customerRef == null) {
            throw new PaymentOperationException(
                    HttpStatus.BAD_GATEWAY,
                    "ORDER_CUSTOMER_REFERENCE_MISSING",
                    "Order service returned an order without a customer reference"
            );
        }

        String requestedCurrency = normalizeCurrencyCode(request.currencyCode());
        String orderCurrency = normalizeCurrencyCode(order.currencyCode());
        if (requestedCurrency != null && !requestedCurrency.equals(orderCurrency)) {
            throw new PaymentOperationException(
                    HttpStatus.BAD_REQUEST,
                    "PAYMENT_CURRENCY_MISMATCH",
                    "Payment currency must match the immutable order currency"
            );
        }

        String idempotencyKey = normalizeNullable(request.idempotencyKey());
        if (idempotencyKey != null) {
            PaymentTransaction existing = paymentTransactionRepository
                    .findByOrderIdAndCustomerRefAndIdempotencyKey(order.id(), customerRef, idempotencyKey)
                    .orElse(null);
            if (existing != null) {
                return toPaymentResponse(existing);
            }
        }

        PaymentProvider provider = paymentProviderRegistry.resolve(request.resolvedProviderCode());
        String paymentReference = generatePaymentReference();
        Map<String, Object> metadata = parseScenarioMetadata(request.metadata());
        PaymentProvider.ProviderOperationResult providerResult = provider.createPaymentIntent(
                new PaymentProvider.CreateRequest(
                        null,
                        order.id(),
                        order.orderNumber(),
                        paymentReference,
                        request.methodType(),
                        normalizeMoney(order.totalAmount()),
                        orderCurrency,
                        request.resolvedAutoCapture(),
                        metadata
                )
        );

        PaymentTransaction payment = new PaymentTransaction();
        payment.setOrderId(order.id());
        payment.setCustomerRef(customerRef);
        payment.setPaymentReference(paymentReference);
        payment.setMethodType(request.methodType());
        payment.setProviderCode(provider.providerCode());
        payment.setIdempotencyKey(idempotencyKey);
        payment.setAmount(normalizeMoney(order.totalAmount()));
        payment.setCurrencyCode(orderCurrency);
        payment.setAutoCapture(request.resolvedAutoCapture());
        payment.setRequestedAt(Instant.now());
        payment.setMetadataJson(writeMetadataJson(metadata));
        payment.setCreatedBy(context.actorId());
        payment.setUpdatedBy(context.actorId());

        applyProviderResult(payment, providerResult, context.actorId(), false);

        try {
            PaymentTransaction saved = paymentTransactionRepository.save(payment);
            log.info("Created payment intent {} for order {} using provider {}",
                    saved.getPaymentReference(), saved.getOrderId(), saved.getProviderCode());
            return toPaymentResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            PaymentTransaction existing = idempotencyKey == null
                    ? null
                    : paymentTransactionRepository.findByOrderIdAndCustomerRefAndIdempotencyKey(order.id(), customerRef, idempotencyKey)
                    .orElse(null);
            if (existing != null) {
                return toPaymentResponse(existing);
            }
            throw ex;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public PaymentResponse confirmPayment(PaymentRequestContext context, UUID paymentId, ConfirmPaymentRequest request) {
        PaymentTransaction payment = paymentTransactionRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new NotFoundException("PAYMENT_NOT_FOUND", "Payment not found"));
        assertPaymentAccess(context, payment);

        PaymentConfirmationAction action = request.resolvedAction();
        if (action == PaymentConfirmationAction.AUTHORIZE && payment.getStatus() == PaymentStatus.AUTHORIZED) {
            return toPaymentResponse(payment);
        }
        if (action == PaymentConfirmationAction.CAPTURE
                && (payment.getStatus() == PaymentStatus.CAPTURED || payment.getStatus() == PaymentStatus.REFUNDED)) {
            return toPaymentResponse(payment);
        }
        if (payment.getStatus() == PaymentStatus.PENDING) {
            return toPaymentResponse(payment);
        }
        if (isTerminal(payment.getStatus())) {
            throw new PaymentOperationException(
                    HttpStatus.BAD_REQUEST,
                    "PAYMENT_CONFIRM_TERMINAL_STATE",
                    "Payment is already in a terminal state and cannot be confirmed"
            );
        }

        PaymentProvider provider = paymentProviderRegistry.resolve(payment.getProviderCode());
        Map<String, Object> metadata = readMetadata(payment.getMetadataJson());
        String providerTransactionId = normalizeNullable(request.providerTransactionId());
        if (providerTransactionId == null) {
            providerTransactionId = normalizeNullable(payment.getProviderTransactionId());
        }

        PaymentProvider.ProviderOperationResult providerResult = switch (action) {
            case AUTHORIZE -> provider.authorizePayment(
                    new PaymentProvider.AuthorizeRequest(
                            payment.getId(),
                            payment.getOrderId(),
                            payment.getPaymentReference(),
                            providerTransactionId,
                            payment.getAmount(),
                            payment.getCurrencyCode(),
                            metadata
                    )
            );
            case CAPTURE -> provider.capturePayment(
                    new PaymentProvider.CaptureRequest(
                            payment.getId(),
                            payment.getOrderId(),
                            payment.getPaymentReference(),
                            providerTransactionId,
                            payment.getAmount(),
                            payment.getCurrencyCode(),
                            metadata
                    )
            );
        };

        applyProviderResult(payment, providerResult, context.actorId(), true);
        payment.setUpdatedBy(context.actorId());
        PaymentTransaction saved = paymentTransactionRepository.save(payment);
        log.info("Confirmed payment {} using action {} and provider {} -> {}",
                saved.getPaymentReference(), action, saved.getProviderCode(), saved.getStatus());
        return toPaymentResponse(saved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(PaymentRequestContext context, UUID paymentId) {
        PaymentTransaction payment = paymentTransactionRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("PAYMENT_NOT_FOUND", "Payment not found"));
        assertPaymentAccess(context, payment);
        return toPaymentResponse(payment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getLatestPaymentByOrderId(PaymentRequestContext context, UUID orderId) {
        PaymentTransaction payment = paymentTransactionRepository.findByOrderIdOrderByUpdatedAtDesc(orderId).stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("PAYMENT_NOT_FOUND", "Payment not found for order"));
        assertPaymentAccess(context, payment);
        return toPaymentResponse(payment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public PaymentResponse updatePaymentStatus(PaymentStatusUpdateRequest request, String actorId) {
        PaymentTransaction payment = resolvePaymentForManualUpdate(request);
        PaymentWebhookEvent event = createWebhookEventIfPresent(payment, request.providerCode(), request.providerEventId(), request.eventType(), actorId);
        PaymentProvider.ProviderOperationResult result = new PaymentProvider.ProviderOperationResult(
                request.status(),
                deriveAuthorizationStatus(payment, request.status()),
                deriveCaptureStatus(payment, request.status()),
                firstNonBlank(request.providerTransactionId(), payment.getProviderTransactionId()),
                resolveManualFailureReason(request)
        );

        mergeMetadata(payment, request.metadata());
        boolean changed = applyProviderResult(payment, result, actorId, true);
        payment.setUpdatedBy(actorId);
        payment.setLastWebhookReceivedAt(Instant.now());
        payment.setLastWebhookProcessedAt(Instant.now());
        PaymentTransaction saved = paymentTransactionRepository.save(payment);
        finalizeWebhookEvent(event, saved, changed, result.failureReason());
        return toPaymentResponse(saved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public PaymentWebhookResponse handleWebhook(String providerCode, String payload, Map<String, String> headers) {
        PaymentProvider provider = paymentProviderRegistry.resolve(providerCode);
        PaymentProvider.ProviderWebhookNotification notification = provider.parseWebhook(
                new PaymentProvider.WebhookRequest(providerCode, payload, headers)
        );

        PaymentWebhookEvent existing = paymentWebhookEventRepository
                .findByProviderCodeAndProviderEventId(notification.providerCode(), notification.providerEventId())
                .orElse(null);
        if (existing != null) {
            return toDuplicateWebhookResponse(existing);
        }

        PaymentWebhookEvent event = new PaymentWebhookEvent();
        event.setProviderCode(notification.providerCode());
        event.setProviderEventId(notification.providerEventId());
        event.setEventType(notification.eventType());
        event.setPaymentReference(notification.paymentReference());
        event.setProviderTransactionId(notification.providerTransactionId());
        event.setProcessingStatus(PaymentWebhookProcessingStatus.RECEIVED);
        event.setSignatureVerified(notification.signatureVerified());
        event.setPayloadJson(notification.payloadJson());
        event.setReceivedAt(Instant.now());
        event.setCreatedBy("webhook:" + notification.providerCode());
        event.setUpdatedBy("webhook:" + notification.providerCode());

        try {
            event = paymentWebhookEventRepository.save(event);
        } catch (DataIntegrityViolationException ex) {
            PaymentWebhookEvent duplicate = paymentWebhookEventRepository
                    .findByProviderCodeAndProviderEventId(notification.providerCode(), notification.providerEventId())
                    .orElse(null);
            if (duplicate != null) {
                return toDuplicateWebhookResponse(duplicate);
            }
            throw ex;
        }

        PaymentTransaction payment = resolvePaymentForWebhook(notification);
        if (payment == null) {
            event.setProcessingStatus(PaymentWebhookProcessingStatus.FAILED);
            event.setFailureReason("No payment found for webhook reference");
            event.setUpdatedBy("webhook:" + notification.providerCode());
            paymentWebhookEventRepository.save(event);
            log.warn("Webhook {} from provider {} could not be matched to a payment",
                    notification.providerEventId(), notification.providerCode());
            return new PaymentWebhookResponse(
                    notification.providerCode(),
                    notification.providerEventId(),
                    notification.eventType(),
                    PaymentWebhookProcessingStatus.FAILED,
                    false,
                    notification.signatureVerified(),
                    null,
                    null,
                    "Webhook recorded without a matching payment"
            );
        }

        PaymentProvider.ProviderOperationResult result = new PaymentProvider.ProviderOperationResult(
                notification.status(),
                notification.authorizationStatus(),
                notification.captureStatus(),
                firstNonBlank(notification.providerTransactionId(), payment.getProviderTransactionId()),
                notification.failureReason()
        );

        PaymentTransaction savedPayment;
        boolean changed;
        try {
            payment.setLastWebhookReceivedAt(Instant.now());
            changed = applyProviderResult(payment, result, "webhook:" + notification.providerCode(), true);
            payment.setLastWebhookProcessedAt(Instant.now());
            payment.setUpdatedBy("webhook:" + notification.providerCode());
            savedPayment = paymentTransactionRepository.save(payment);
        } catch (PaymentOperationException ex) {
            event.setProcessingStatus(PaymentWebhookProcessingStatus.FAILED);
            event.setFailureReason(ex.getMessage());
            event.setProcessedAt(Instant.now());
            event.setUpdatedBy("webhook:" + notification.providerCode());
            paymentWebhookEventRepository.save(event);
            log.warn("Webhook {} from provider {} was rejected: {}",
                    notification.providerEventId(), notification.providerCode(), ex.getMessage());
            return new PaymentWebhookResponse(
                    notification.providerCode(),
                    notification.providerEventId(),
                    notification.eventType(),
                    PaymentWebhookProcessingStatus.FAILED,
                    false,
                    notification.signatureVerified(),
                    payment.getId(),
                    payment.getStatus(),
                    ex.getMessage()
            );
        }

        finalizeWebhookEvent(event, savedPayment, changed, notification.failureReason());
        log.info("Processed webhook {} for payment {} -> {}",
                notification.providerEventId(), savedPayment.getPaymentReference(), savedPayment.getStatus());

        return new PaymentWebhookResponse(
                notification.providerCode(),
                notification.providerEventId(),
                notification.eventType(),
                changed ? PaymentWebhookProcessingStatus.PROCESSED : PaymentWebhookProcessingStatus.IGNORED,
                false,
                notification.signatureVerified(),
                savedPayment.getId(),
                savedPayment.getStatus(),
                changed ? "Webhook processed" : "Webhook accepted without state change"
        );
    }

    /**
     * Resolves the payment targeted by an internal/manual status update.
     *
     * @param request status update payload
     * @return locked payment transaction
     */
    private PaymentTransaction resolvePaymentForManualUpdate(PaymentStatusUpdateRequest request) {
        if (request.paymentId() != null) {
            return paymentTransactionRepository.findByIdForUpdate(request.paymentId())
                    .orElseThrow(() -> new NotFoundException("PAYMENT_NOT_FOUND", "Payment not found"));
        }
        String paymentReference = normalizeNullable(request.paymentReference());
        if (paymentReference != null) {
            return paymentTransactionRepository.findByPaymentReferenceForUpdate(paymentReference)
                    .orElseThrow(() -> new NotFoundException("PAYMENT_NOT_FOUND", "Payment not found"));
        }
        throw new PaymentOperationException(
                HttpStatus.BAD_REQUEST,
                "PAYMENT_IDENTIFIER_REQUIRED",
                "Status update requires paymentId or paymentReference"
        );
    }

    /**
     * Resolves the payment targeted by a provider webhook.
     *
     * @param notification normalized webhook notification
     * @return locked payment or {@code null}
     */
    private PaymentTransaction resolvePaymentForWebhook(PaymentProvider.ProviderWebhookNotification notification) {
        if (notification.paymentReference() != null) {
            return paymentTransactionRepository.findByPaymentReferenceForUpdate(notification.paymentReference()).orElse(null);
        }
        if (notification.providerTransactionId() != null) {
            return paymentTransactionRepository
                    .findByProviderCodeAndProviderTransactionIdForUpdate(notification.providerCode(), notification.providerTransactionId())
                    .orElse(null);
        }
        return null;
    }

    /**
     * Creates a webhook event record for manual status updates when a provider event ID is supplied.
     *
     * @param payment target payment
     * @param providerCode provider code
     * @param providerEventId provider event identifier
     * @param eventType event type label
     * @param actorId audit actor identifier
     * @return persisted event or {@code null}
     */
    private PaymentWebhookEvent createWebhookEventIfPresent(
            PaymentTransaction payment,
            String providerCode,
            String providerEventId,
            String eventType,
            String actorId
    ) {
        String normalizedEventId = normalizeNullable(providerEventId);
        if (normalizedEventId == null) {
            return null;
        }
        String normalizedProviderCode = normalizeProviderCode(firstNonBlank(providerCode, payment.getProviderCode()));
        PaymentWebhookEvent existing = paymentWebhookEventRepository
                .findByProviderCodeAndProviderEventId(normalizedProviderCode, normalizedEventId)
                .orElse(null);
        if (existing != null) {
            throw new PaymentOperationException(
                    HttpStatus.CONFLICT,
                    "PAYMENT_STATUS_EVENT_DUPLICATE",
                    "The provided provider event has already been processed"
            );
        }

        PaymentWebhookEvent event = new PaymentWebhookEvent();
        event.setPaymentTransaction(payment);
        event.setProviderCode(normalizedProviderCode);
        event.setProviderEventId(normalizedEventId);
        event.setEventType(firstNonBlank(eventType, "internal.status-update"));
        event.setPaymentReference(payment.getPaymentReference());
        event.setProviderTransactionId(payment.getProviderTransactionId());
        event.setProcessingStatus(PaymentWebhookProcessingStatus.RECEIVED);
        event.setSignatureVerified(true);
        event.setPayloadJson("{\"source\":\"internal-status-update\"}");
        event.setReceivedAt(Instant.now());
        event.setCreatedBy(actorId);
        event.setUpdatedBy(actorId);
        return paymentWebhookEventRepository.save(event);
    }

    /**
     * Finalizes webhook event processing state.
     *
     * @param event webhook event record
     * @param payment linked payment
     * @param changed whether payment state changed
     * @param failureReason optional failure reason
     */
    private void finalizeWebhookEvent(
            PaymentWebhookEvent event,
            PaymentTransaction payment,
            boolean changed,
            String failureReason
    ) {
        if (event == null) {
            return;
        }
        event.setPaymentTransaction(payment);
        event.setPaymentReference(payment.getPaymentReference());
        event.setProviderTransactionId(payment.getProviderTransactionId());
        event.setProcessingStatus(changed ? PaymentWebhookProcessingStatus.PROCESSED : PaymentWebhookProcessingStatus.IGNORED);
        event.setFailureReason(normalizeNullable(failureReason));
        event.setProcessedAt(Instant.now());
        event.setUpdatedBy(payment.getUpdatedBy());
        paymentWebhookEventRepository.save(event);
    }

    /**
     * Applies provider-side normalized state to one payment aggregate.
     *
     * @param payment target payment
     * @param result normalized provider result
     * @param actorId actor identifier for audit fields
     * @param markConfirmed whether the flow counts as a confirm/update attempt
     * @return {@code true} when any relevant payment state changed
     */
    private boolean applyProviderResult(
            PaymentTransaction payment,
            PaymentProvider.ProviderOperationResult result,
            String actorId,
            boolean markConfirmed
    ) {
        PaymentStatus targetStatus = result.status();
        validateStatusTransition(payment.getStatus(), targetStatus);

        Instant now = Instant.now();
        boolean changed = false;

        changed |= assignIfDifferent(payment::getStatus, payment::setStatus, targetStatus);
        changed |= assignIfDifferent(
                payment::getAuthorizationStatus,
                payment::setAuthorizationStatus,
                firstNonNull(result.authorizationStatus(), deriveAuthorizationStatus(payment, targetStatus))
        );
        changed |= assignIfDifferent(
                payment::getCaptureStatus,
                payment::setCaptureStatus,
                firstNonNull(result.captureStatus(), deriveCaptureStatus(payment, targetStatus))
        );
        changed |= assignIfDifferent(
                payment::getProviderTransactionId,
                payment::setProviderTransactionId,
                normalizeNullable(result.providerTransactionId())
        );

        if (markConfirmed && payment.getConfirmedAt() == null) {
            payment.setConfirmedAt(now);
            changed = true;
        }

        switch (targetStatus) {
            case CREATED, REQUIRES_CONFIRMATION -> {
                changed |= assignIfDifferent(payment::getFailureReason, payment::setFailureReason, null);
            }
            case PENDING -> {
                changed |= assignIfDifferent(payment::getFailureReason, payment::setFailureReason, null);
            }
            case AUTHORIZED -> {
                changed |= assignIfDifferent(payment::getAuthorizedAt, payment::setAuthorizedAt, firstNonNull(payment.getAuthorizedAt(), now));
                changed |= assignIfDifferent(payment::getAuthorizedAmount, payment::setAuthorizedAmount, payment.getAmount());
                changed |= assignIfDifferent(payment::getFailureReason, payment::setFailureReason, null);
            }
            case CAPTURED -> {
                changed |= assignIfDifferent(payment::getAuthorizedAt, payment::setAuthorizedAt, firstNonNull(payment.getAuthorizedAt(), now));
                changed |= assignIfDifferent(payment::getAuthorizedAmount, payment::setAuthorizedAmount, payment.getAmount());
                changed |= assignIfDifferent(payment::getCapturedAt, payment::setCapturedAt, firstNonNull(payment.getCapturedAt(), now));
                changed |= assignIfDifferent(payment::getCapturedAmount, payment::setCapturedAmount, payment.getAmount());
                changed |= assignIfDifferent(payment::getFailureReason, payment::setFailureReason, null);
                changed |= assignIfDifferent(payment::getCompletedAt, payment::setCompletedAt, firstNonNull(payment.getCompletedAt(), now));
            }
            case REFUNDED -> {
                changed |= assignIfDifferent(payment::getAuthorizedAt, payment::setAuthorizedAt, firstNonNull(payment.getAuthorizedAt(), now));
                changed |= assignIfDifferent(payment::getCapturedAt, payment::setCapturedAt, firstNonNull(payment.getCapturedAt(), now));
                changed |= assignIfDifferent(payment::getAuthorizedAmount, payment::setAuthorizedAmount, firstNonNull(payment.getAuthorizedAmount(), payment.getAmount()));
                changed |= assignIfDifferent(payment::getCapturedAmount, payment::setCapturedAmount, firstNonNull(payment.getCapturedAmount(), payment.getAmount()));
                changed |= assignIfDifferent(payment::getRefundedAmount, payment::setRefundedAmount, firstNonNull(payment.getRefundedAmount(), firstNonNull(payment.getCapturedAmount(), payment.getAmount())));
                changed |= assignIfDifferent(payment::getFailureReason, payment::setFailureReason, null);
                changed |= assignIfDifferent(payment::getCompletedAt, payment::setCompletedAt, firstNonNull(payment.getCompletedAt(), now));
            }
            case FAILED -> {
                changed |= assignIfDifferent(payment::getFailureReason, payment::setFailureReason, firstNonBlank(result.failureReason(), "Payment failed"));
                changed |= assignIfDifferent(payment::getCompletedAt, payment::setCompletedAt, firstNonNull(payment.getCompletedAt(), now));
            }
            case CANCELED -> {
                changed |= assignIfDifferent(payment::getFailureReason, payment::setFailureReason, firstNonBlank(result.failureReason(), "Payment canceled"));
                changed |= assignIfDifferent(payment::getCompletedAt, payment::setCompletedAt, firstNonNull(payment.getCompletedAt(), now));
            }
        }

        if (targetStatus != PaymentStatus.CAPTURED
                && targetStatus != PaymentStatus.REFUNDED
                && targetStatus != PaymentStatus.FAILED
                && targetStatus != PaymentStatus.CANCELED) {
            changed |= assignIfDifferent(payment::getCompletedAt, payment::setCompletedAt, null);
        }

        payment.setUpdatedBy(actorId);
        return changed;
    }

    /**
     * Enforces order-level access rules using returned order ownership data.
     *
     * @param context request context
     * @param order target order
     */
    private void assertOrderAccess(PaymentRequestContext context, OrderServiceClient.OrderPayload order) {
        if (context != null && context.canManageAllPayments()) {
            return;
        }
        if (context == null || !context.hasSubject()) {
            throw new PaymentOperationException(
                    HttpStatus.UNAUTHORIZED,
                    "AUTH_SUBJECT_REQUIRED",
                    "Authenticated customer identity is required"
            );
        }
        if (!context.subject().equals(normalizeNullable(order.customerRef()))) {
            throw new PaymentOperationException(
                    HttpStatus.FORBIDDEN,
                    "PAYMENT_ORDER_FORBIDDEN",
                    "Order access is forbidden for the current actor"
            );
        }
    }

    /**
     * Enforces payment read/update access.
     *
     * @param context request context
     * @param payment target payment
     */
    private void assertPaymentAccess(PaymentRequestContext context, PaymentTransaction payment) {
        if (context != null && context.canManageAllPayments()) {
            return;
        }
        if (context == null || !context.hasSubject()) {
            throw new PaymentOperationException(
                    HttpStatus.UNAUTHORIZED,
                    "AUTH_SUBJECT_REQUIRED",
                    "Authenticated customer identity is required"
            );
        }
        if (!context.subject().equals(payment.getCustomerRef())) {
            throw new PaymentOperationException(
                    HttpStatus.FORBIDDEN,
                    "PAYMENT_FORBIDDEN",
                    "Payment access is forbidden"
            );
        }
    }

    /**
     * Validates requested payment status transition.
     *
     * @param current current status
     * @param next requested next status
     */
    private void validateStatusTransition(PaymentStatus current, PaymentStatus next) {
        if (current == next) {
            return;
        }
        Set<PaymentStatus> allowed = STATUS_FLOW.getOrDefault(current, Set.of());
        if (!allowed.contains(next)) {
            throw new PaymentOperationException(
                    HttpStatus.BAD_REQUEST,
                    "PAYMENT_STATUS_INVALID_TRANSITION",
                    "Payment cannot transition from %s to %s".formatted(current, next)
            );
        }
    }

    /**
     * Returns whether one payment status is terminal.
     *
     * @param status payment status
     * @return {@code true} when terminal
     */
    private boolean isTerminal(PaymentStatus status) {
        return status == PaymentStatus.CAPTURED
                || status == PaymentStatus.REFUNDED
                || status == PaymentStatus.FAILED
                || status == PaymentStatus.CANCELED;
    }

    /**
     * Generates a readable internal payment reference.
     *
     * @return payment reference
     */
    private String generatePaymentReference() {
        return "pay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toLowerCase(Locale.ROOT);
    }

    /**
     * Reads current correlation ID from MDC.
     *
     * @return correlation ID or {@code null}
     */
    private String currentCorrelationId() {
        return normalizeNullable(MDC.get("correlationId"));
    }

    /**
     * Converts one payment entity to response DTO.
     *
     * @param payment source entity
     * @return response DTO
     */
    private PaymentResponse toPaymentResponse(PaymentTransaction payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getCustomerRef(),
                payment.getPaymentReference(),
                payment.getMethodType(),
                payment.getStatus(),
                payment.getAuthorizationStatus(),
                payment.getCaptureStatus(),
                payment.getProviderCode(),
                payment.getProviderTransactionId(),
                payment.getIdempotencyKey(),
                payment.getAmount(),
                payment.getAuthorizedAmount(),
                payment.getCapturedAmount(),
                payment.getRefundedAmount(),
                payment.getCurrencyCode(),
                payment.isAutoCapture(),
                payment.getRequestedAt(),
                payment.getConfirmedAt(),
                payment.getAuthorizedAt(),
                payment.getCapturedAt(),
                payment.getCompletedAt(),
                payment.getLastWebhookReceivedAt(),
                payment.getLastWebhookProcessedAt(),
                payment.getFailureReason(),
                readMetadata(payment.getMetadataJson()),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }

    /**
     * Converts an existing webhook event into a duplicate-delivery response.
     *
     * @param existing existing webhook event
     * @return duplicate response
     */
    private PaymentWebhookResponse toDuplicateWebhookResponse(PaymentWebhookEvent existing) {
        PaymentTransaction payment = existing.getPaymentTransaction();
        return new PaymentWebhookResponse(
                existing.getProviderCode(),
                existing.getProviderEventId(),
                existing.getEventType(),
                existing.getProcessingStatus(),
                true,
                existing.isSignatureVerified(),
                payment == null ? null : payment.getId(),
                payment == null ? null : payment.getStatus(),
                "Webhook delivery was already processed"
        );
    }

    /**
     * Parses request metadata into a stable mutable map.
     *
     * @param metadata source metadata
     * @return mutable metadata map
     */
    private Map<String, Object> parseScenarioMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return new LinkedHashMap<>(metadata);
    }

    /**
     * Merges request metadata into the persisted payment metadata document.
     *
     * @param payment target payment
     * @param incoming incoming metadata
     */
    private void mergeMetadata(PaymentTransaction payment, Map<String, Object> incoming) {
        if (incoming == null || incoming.isEmpty()) {
            return;
        }
        Map<String, Object> merged = new LinkedHashMap<>(readMetadata(payment.getMetadataJson()));
        merged.putAll(incoming);
        payment.setMetadataJson(writeMetadataJson(merged));
    }

    /**
     * Reads metadata JSON into a map.
     *
     * @param metadataJson raw JSON
     * @return metadata map
     */
    private Map<String, Object> readMetadata(String metadataJson) {
        String normalized = normalizeNullable(metadataJson);
        if (normalized == null) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(normalized, MAP_TYPE);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to parse payment metadata JSON for response rendering: {}", ex.getMessage());
            return Map.of();
        }
    }

    /**
     * Serializes metadata map to JSON text.
     *
     * @param metadata metadata map
     * @return serialized JSON or {@code null}
     */
    private String writeMetadataJson(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException ex) {
            throw new PaymentOperationException(
                    HttpStatus.BAD_REQUEST,
                    "PAYMENT_METADATA_INVALID",
                    "Payment metadata could not be serialized"
            );
        }
    }

    /**
     * Derives authorization status for a target payment state.
     *
     * @param payment current payment
     * @param targetStatus target state
     * @return authorization status
     */
    private PaymentAuthorizationStatus deriveAuthorizationStatus(PaymentTransaction payment, PaymentStatus targetStatus) {
        return switch (targetStatus) {
            case CREATED, REQUIRES_CONFIRMATION -> PaymentAuthorizationStatus.NOT_REQUESTED;
            case PENDING -> payment.getAuthorizationStatus() == PaymentAuthorizationStatus.AUTHORIZED
                    ? PaymentAuthorizationStatus.AUTHORIZED
                    : PaymentAuthorizationStatus.PENDING;
            case AUTHORIZED, CAPTURED, REFUNDED -> PaymentAuthorizationStatus.AUTHORIZED;
            case FAILED -> payment.getAuthorizationStatus() == PaymentAuthorizationStatus.AUTHORIZED
                    ? PaymentAuthorizationStatus.AUTHORIZED
                    : PaymentAuthorizationStatus.FAILED;
            case CANCELED -> PaymentAuthorizationStatus.CANCELED;
        };
    }

    /**
     * Derives capture status for a target payment state.
     *
     * @param payment current payment
     * @param targetStatus target state
     * @return capture status
     */
    private PaymentCaptureStatus deriveCaptureStatus(PaymentTransaction payment, PaymentStatus targetStatus) {
        return switch (targetStatus) {
            case CREATED, REQUIRES_CONFIRMATION, AUTHORIZED -> PaymentCaptureStatus.NOT_CAPTURED;
            case PENDING -> payment.getAuthorizationStatus() == PaymentAuthorizationStatus.AUTHORIZED
                    ? PaymentCaptureStatus.PENDING
                    : PaymentCaptureStatus.NOT_CAPTURED;
            case CAPTURED -> PaymentCaptureStatus.CAPTURED;
            case REFUNDED -> PaymentCaptureStatus.REFUNDED;
            case FAILED -> payment.getAuthorizationStatus() == PaymentAuthorizationStatus.AUTHORIZED
                    ? PaymentCaptureStatus.FAILED
                    : PaymentCaptureStatus.NOT_CAPTURED;
            case CANCELED -> PaymentCaptureStatus.CANCELED;
        };
    }

    /**
     * Resolves failure reason for manual status updates.
     *
     * @param request update request
     * @return failure reason or {@code null}
     */
    private String resolveManualFailureReason(PaymentStatusUpdateRequest request) {
        String failureReason = normalizeNullable(request.failureReason());
        if (request.status() == PaymentStatus.FAILED && failureReason == null) {
            return "Payment marked as failed by internal status update";
        }
        if (request.status() == PaymentStatus.CANCELED && failureReason == null) {
            return "Payment canceled by internal status update";
        }
        return failureReason;
    }

    /**
     * Normalizes provider code.
     *
     * @param value source value
     * @return normalized provider code
     */
    private String normalizeProviderCode(String value) {
        String normalized = normalizeNullable(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    /**
     * Normalizes currency code while preserving null when absent.
     *
     * @param value source value
     * @return normalized currency code
     */
    private String normalizeCurrencyCode(String value) {
        String normalized = normalizeNullable(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    /**
     * Normalizes monetary values to scale 4.
     *
     * @param value source money
     * @return normalized money
     */
    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return ZERO;
        }
        return value.setScale(4, RoundingMode.HALF_UP);
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

    /**
     * Returns the first non-blank string in order.
     *
     * @param values candidate values
     * @return first non-blank value or {@code null}
     */
    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = normalizeNullable(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    /**
     * Returns the first non-null candidate.
     *
     * @param first first candidate
     * @param second fallback candidate
     * @param <T> value type
     * @return resolved value
     */
    private <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }

    /**
     * Assigns a new value when it differs from the current value.
     *
     * @param getter current value supplier
     * @param setter target setter
     * @param value new value
     * @param <T> value type
     * @return {@code true} when assignment changed state
     */
    private <T> boolean assignIfDifferent(ValueGetter<T> getter, ValueSetter<T> setter, T value) {
        T current = getter.get();
        if (current == null ? value == null : current.equals(value)) {
            return false;
        }
        setter.set(value);
        return true;
    }

    @FunctionalInterface
    private interface ValueGetter<T> {
        T get();
    }

    @FunctionalInterface
    private interface ValueSetter<T> {
        void set(T value);
    }
}
