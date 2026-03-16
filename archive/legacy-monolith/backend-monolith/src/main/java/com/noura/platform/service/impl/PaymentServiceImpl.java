package com.noura.platform.service.impl;

import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.domain.entity.Order;
import com.noura.platform.domain.entity.PaymentTransaction;
import com.noura.platform.domain.entity.UserAccount;
import com.noura.platform.domain.enums.OrderStatus;
import com.noura.platform.domain.enums.PaymentStatus;
import com.noura.platform.dto.payment.CreatePaymentRequest;
import com.noura.platform.dto.payment.MockPaymentWebhookRequest;
import com.noura.platform.dto.payment.PaymentResponse;
import com.noura.platform.repository.OrderRepository;
import com.noura.platform.repository.PaymentTransactionRepository;
import com.noura.platform.repository.UserAccountRepository;
import com.noura.platform.security.SecurityUtils;
import com.noura.platform.service.PaymentService;
import com.noura.platform.service.payment.PaymentGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final OrderRepository orderRepository;
    private final UserAccountRepository userAccountRepository;
    private final List<PaymentGateway> paymentGateways;

    @Override
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        UserAccount user = currentUser();
        Order order = requireOwnedOrder(request.orderId(), user);
        validateOrderForPayment(order);

        String providerCode = normalizeProviderCode(request.resolvedProviderCode());
        if (providerCode == null) {
            throw new BadRequestException("PAYMENT_PROVIDER_REQUIRED", "Provider code is required");
        }

        PaymentGateway gateway = resolveGateway(providerCode);
        String paymentReference = generatePaymentReference();
        String currencyCode = resolveCurrencyCode(request.currencyCode());

        PaymentGateway.GatewayCreateResult gatewayResult = gateway.createPayment(
                new PaymentGateway.GatewayCreateRequest(
                        order.getId(),
                        paymentReference,
                        request.methodType(),
                        order.getTotalAmount(),
                        currencyCode
                )
        );

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setOrder(order);
        transaction.setPaymentReference(paymentReference);
        transaction.setMethodType(request.methodType());
        transaction.setStatus(gatewayResult.status() == null ? PaymentStatus.PENDING : gatewayResult.status());
        transaction.setProviderCode(providerCode);
        transaction.setProviderTransactionId(trimToNull(gatewayResult.providerTransactionId()));
        transaction.setAmount(order.getTotalAmount());
        transaction.setCurrencyCode(currencyCode);
        transaction.setRequestedAt(Instant.now());
        transaction.setFailureReason(trimToNull(gatewayResult.failureReason()));
        if (transaction.getStatus() != PaymentStatus.PENDING) {
            transaction.setCompletedAt(Instant.now());
        }

        PaymentTransaction saved = paymentTransactionRepository.save(transaction);
        applyOrderStateAfterCreate(order, saved);
        orderRepository.save(order);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public PaymentResponse getPayment(UUID paymentId) {
        String email = SecurityUtils.currentEmail();
        PaymentTransaction transaction = paymentTransactionRepository.findByIdAndOrder_User_EmailIgnoreCase(paymentId, email)
                .orElseThrow(() -> new NotFoundException("PAYMENT_NOT_FOUND", "Payment transaction not found"));
        return toResponse(transaction);
    }

    @Override
    @Transactional
    public PaymentResponse processMockWebhook(MockPaymentWebhookRequest request) {
        PaymentGateway gateway = resolveGateway("mock");
        PaymentTransaction transaction = paymentTransactionRepository
                .findByPaymentReferenceAndProviderCodeIgnoreCase(request.paymentReference().trim(), gateway.providerCode())
                .orElseThrow(() -> new NotFoundException("PAYMENT_NOT_FOUND", "Payment transaction not found"));

        PaymentGateway.GatewayWebhookResult gatewayResult = gateway.processWebhook(
                new PaymentGateway.GatewayWebhookRequest(
                        request.paymentReference().trim(),
                        request.status(),
                        trimToNull(request.providerTransactionId()),
                        trimToNull(request.failureReason())
                )
        );

        if (transaction.getStatus() != PaymentStatus.PENDING) {
            if (transaction.getStatus() == gatewayResult.status()) {
                return toResponse(transaction);
            }
            throw new BadRequestException("PAYMENT_ALREADY_FINALIZED", "Payment transaction is already finalized");
        }

        transaction.setStatus(gatewayResult.status());
        transaction.setProviderTransactionId(trimToNull(gatewayResult.providerTransactionId()));
        transaction.setFailureReason(gatewayResult.status() == PaymentStatus.FAILED
                ? trimToNull(gatewayResult.failureReason())
                : null);
        transaction.setCompletedAt(Instant.now());

        PaymentTransaction saved = paymentTransactionRepository.save(transaction);
        applyOrderStateAfterWebhook(saved.getOrder(), saved);
        orderRepository.save(saved.getOrder());
        return toResponse(saved);
    }

    private UserAccount currentUser() {
        return userAccountRepository.findByEmailIgnoreCase(SecurityUtils.currentEmail())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
    }

    private Order requireOwnedOrder(UUID orderId, UserAccount user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));
        if (order.getUser() == null || !order.getUser().getId().equals(user.getId())) {
            throw new NotFoundException("ORDER_NOT_FOUND", "Order not found");
        }
        return order;
    }

    private void validateOrderForPayment(Order order) {
        if (order.getTotalAmount() == null || order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("ORDER_TOTAL_INVALID", "Order total must be positive");
        }
        if (order.getStatus() == OrderStatus.PAID) {
            throw new BadRequestException("ORDER_ALREADY_PAID", "Order is already paid");
        }
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REFUNDED) {
            throw new BadRequestException("ORDER_PAYMENT_NOT_ALLOWED", "Order cannot accept payments in its current state");
        }
    }

    private PaymentGateway resolveGateway(String providerCode) {
        return paymentGateways.stream()
                .filter(gateway -> gateway.providerCode().equalsIgnoreCase(providerCode))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("PAYMENT_PROVIDER_UNSUPPORTED", "Unsupported payment provider: " + providerCode));
    }

    private void applyOrderStateAfterCreate(Order order, PaymentTransaction transaction) {
        order.setPaymentReference(transaction.getPaymentReference());
        if (transaction.getStatus() == PaymentStatus.SUCCESS) {
            order.setStatus(OrderStatus.PAID);
            return;
        }
        if (order.getStatus() == OrderStatus.CREATED || order.getStatus() == OrderStatus.REVIEWED) {
            order.setStatus(OrderStatus.PAYMENT_PENDING);
        }
    }

    private void applyOrderStateAfterWebhook(Order order, PaymentTransaction transaction) {
        order.setPaymentReference(transaction.getPaymentReference());
        if (transaction.getStatus() == PaymentStatus.SUCCESS) {
            order.setStatus(OrderStatus.PAID);
            return;
        }
        if (transaction.getStatus() == PaymentStatus.FAILED
                && order.getStatus() != OrderStatus.CANCELLED
                && order.getStatus() != OrderStatus.REFUNDED
                && order.getStatus() != OrderStatus.PAID) {
            order.setStatus(OrderStatus.PAYMENT_PENDING);
        }
    }

    private PaymentResponse toResponse(PaymentTransaction transaction) {
        UUID orderId = transaction.getOrderId();
        if (orderId == null && transaction.getOrder() != null) {
            orderId = transaction.getOrder().getId();
        }
        return new PaymentResponse(
                transaction.getId(),
                orderId,
                transaction.getPaymentReference(),
                transaction.getMethodType(),
                transaction.getStatus(),
                transaction.getProviderCode(),
                transaction.getProviderTransactionId(),
                transaction.getAmount(),
                transaction.getCurrencyCode(),
                transaction.getRequestedAt(),
                transaction.getCompletedAt(),
                transaction.getFailureReason(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt()
        );
    }

    private String resolveCurrencyCode(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return "USD";
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeProviderCode(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String generatePaymentReference() {
        return "PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
