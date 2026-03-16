package com.noura.platform.commerce.payments.application;

import com.noura.platform.commerce.orders.domain.Order;
import com.noura.platform.commerce.orders.domain.OrderStatus;
import com.noura.platform.commerce.orders.infrastructure.OrderRepo;
import com.noura.platform.commerce.payments.domain.PaymentTransaction;
import com.noura.platform.commerce.payments.domain.PaymentTransactionStatus;
import com.noura.platform.commerce.payments.infrastructure.PaymentTransactionRepo;
import com.noura.platform.dto.payment.CreatePaymentRequest;
import com.noura.platform.dto.payment.PaymentTransactionResult;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class StorefrontPaymentService {
    private static final String DEFAULT_CURRENCY = "USD";
    private static final String CASH_METHOD = "CASH_ON_DELIVERY";
    private static final int METHOD_MAX_LENGTH = 64;
    private static final int PROVIDER_MAX_LENGTH = 64;
    private static final int REFERENCE_MAX_LENGTH = 128;
    private static final BigDecimal ZERO_MONEY = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
    private final PaymentTransactionRepo paymentTransactionRepo;
    private final OrderRepo orderRepo;

    public StorefrontPaymentService(PaymentTransactionRepo paymentTransactionRepo,
                                   OrderRepo orderRepo) {
        this.paymentTransactionRepo = paymentTransactionRepo;
        this.orderRepo = orderRepo;
    }

    public List<PaymentTransactionResult> listForOrder(Long customerId, Long orderId) {
        verifyOrderOwnership(orderId, customerId);
        return paymentTransactionRepo.findByOrder_IdOrderByCreatedAtDesc(orderId)
                .stream()
                .map(this::toResult)
                .toList();
    }

    public PaymentTransactionResult createInitialPayment(Long customerId, Long orderId, CreatePaymentRequest request) {
        String paymentMethod = normalizeMethod(request == null ? null : request.paymentMethod());
        String provider = normalizeString(request == null ? null : request.provider(), PROVIDER_MAX_LENGTH);
        String providerReference = normalizeString(
                request == null ? null : request.providerReference(),
                REFERENCE_MAX_LENGTH
        );

        Order order = resolveOrder(orderId, customerId);
        return toResult(createForOrder(order, paymentMethod, provider, providerReference));
    }

    public PaymentTransactionResult capture(Long customerId, Long orderId, Long paymentId) {
        Order order = resolveOrder(orderId, customerId);
        PaymentTransaction payment = paymentTransactionRepo.findByOrder_IdAndIdAndOrder_CustomerAccount_Id(
                        orderId, paymentId, customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found."));
        if (payment.getStatus() == PaymentTransactionStatus.CAPTURED) {
            return toResult(payment);
        }
        payment.setStatus(PaymentTransactionStatus.CAPTURED);
        payment.setUpdatedAt(LocalDateTime.now());
        payment.setFailureReason(null);
        payment.setRawResponse(appendResponse(payment.getRawResponse(), "captured"));
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT || order.getStatus() == OrderStatus.DRAFT) {
            order.setStatus(OrderStatus.PAID);
        }
        return toResult(payment);
    }

    public List<PaymentTransactionResult> refundAll(Long customerId, Long orderId, String reason) {
        Order order = resolveOrder(orderId, customerId);
        if (order == null) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now();
        List<PaymentTransaction> transactions = paymentTransactionRepo.findByOrder_IdOrderByCreatedAtDesc(order.getId());
        for (PaymentTransaction payment : transactions) {
            if (payment == null || payment.getStatus() == PaymentTransactionStatus.REFUNDED) {
                continue;
            }
            if (payment.getStatus() == PaymentTransactionStatus.FAILED) {
                continue;
            }
            if (payment.getStatus() == PaymentTransactionStatus.PENDING
                    || payment.getStatus() == PaymentTransactionStatus.AUTHORIZED
                    || payment.getStatus() == PaymentTransactionStatus.CAPTURED) {
                payment.setStatus(PaymentTransactionStatus.REFUNDED);
                payment.setFailureReason(reason == null || reason.isBlank() ? "Order cancelled." : reason.trim());
                payment.setRawResponse(appendResponse(payment.getRawResponse(), "refunded"));
                payment.setUpdatedAt(now);
            }
        }

        return transactions.stream()
                .map(this::toResult)
                .toList();
    }

    public PaymentTransaction createForOrder(Order order, String paymentMethod, String provider, String providerReference) {
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found.");
        }
        if (order.getGrandTotal() == null || order.getGrandTotal().compareTo(ZERO_MONEY) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid order total.");
        }

        String normalizedMethod = normalizeMethod(paymentMethod);
        String normalizedCurrency = normalizeCurrency(order.getCurrencyCode());
        PaymentTransactionStatus initialStatus = isAutomaticCapture(normalizedMethod)
                ? PaymentTransactionStatus.CAPTURED
                : PaymentTransactionStatus.PENDING;

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setOrder(order);
        transaction.setPaymentMethod(normalizedMethod);
        transaction.setProvider(provider);
        transaction.setStatus(initialStatus);
        transaction.setAmount(order.getGrandTotal());
        transaction.setCurrencyCode(normalizedCurrency);
        transaction.setProviderReference(providerReference);
        transaction.setFailureReason(null);
        transaction.setRawResponse(initialStatus == PaymentTransactionStatus.PENDING ? null : "auto-captured");
        PaymentTransaction saved = paymentTransactionRepo.save(transaction);

        if (initialStatus == PaymentTransactionStatus.CAPTURED && order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            order.setStatus(OrderStatus.PAID);
        }
        return saved;
    }

    public PaymentTransactionResult getLatestForOrder(Order order) {
        return paymentTransactionRepo.findFirstByOrder_IdOrderByCreatedAtDesc(order.getId())
                .map(this::toResult)
                .orElse(null);
    }

    public List<PaymentTransactionResult> listByOrder(Order order) {
        return paymentTransactionRepo.findByOrder_IdOrderByCreatedAtDesc(order.getId())
                .stream()
                .map(this::toResult)
                .toList();
    }

    private Order resolveOrder(Long orderId, Long customerId) {
        if (orderId == null || orderId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid order id.");
        }
        return orderRepo.findById(orderId)
                .filter(order -> order.getCustomerAccount() != null
                        && customerId != null
                        && customerId.equals(order.getCustomerAccount().getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found."));
    }

    private void verifyOrderOwnership(Long orderId, Long customerId) {
        resolveOrder(orderId, customerId);
    }

    private boolean isAutomaticCapture(String paymentMethod) {
        return CASH_METHOD.equalsIgnoreCase(paymentMethod)
                || "COD".equalsIgnoreCase(paymentMethod)
                || "CASH".equalsIgnoreCase(paymentMethod);
    }

    private String normalizeMethod(String paymentMethod) {
        String normalized = normalizeString(paymentMethod, METHOD_MAX_LENGTH);
        if (normalized == null || normalized.isBlank()) {
            return CASH_METHOD;
        }
        return normalized.toUpperCase();
    }

    private String normalizeCurrency(String currencyCode) {
        String normalized = normalizeString(currencyCode, 3);
        if (normalized == null || normalized.isBlank()) {
            return DEFAULT_CURRENCY;
        }
        return normalized.toUpperCase();
    }

    private String normalizeString(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            return null;
        }
        if (trimmed.length() > maxLength) {
            return trimmed.substring(0, maxLength);
        }
        return trimmed;
    }

    private String appendResponse(String rawResponse, String event) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return event;
        }
        return rawResponse + ", " + event;
    }

    private PaymentTransactionResult toResult(PaymentTransaction transaction) {
        return new PaymentTransactionResult(
                transaction.getId(),
                transaction.getProvider(),
                transaction.getPaymentMethod(),
                transaction.getStatus().name(),
                transaction.getAmount(),
                transaction.getCurrencyCode(),
                transaction.getProviderReference(),
                transaction.getFailureReason(),
                transaction.getCreatedAt() == null ? null : transaction.getCreatedAt(),
                transaction.getUpdatedAt() == null ? null : transaction.getUpdatedAt()
        );
    }

}
