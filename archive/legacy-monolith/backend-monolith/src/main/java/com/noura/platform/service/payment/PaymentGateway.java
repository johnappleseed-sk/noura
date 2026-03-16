package com.noura.platform.service.payment;

import com.noura.platform.domain.enums.PaymentMethodType;
import com.noura.platform.domain.enums.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentGateway {
    String providerCode();

    GatewayCreateResult createPayment(GatewayCreateRequest request);

    GatewayWebhookResult processWebhook(GatewayWebhookRequest request);

    record GatewayCreateRequest(
            UUID orderId,
            String paymentReference,
            PaymentMethodType methodType,
            BigDecimal amount,
            String currencyCode
    ) {
    }

    record GatewayCreateResult(
            PaymentStatus status,
            String providerTransactionId,
            String failureReason
    ) {
    }

    record GatewayWebhookRequest(
            String paymentReference,
            PaymentStatus status,
            String providerTransactionId,
            String failureReason
    ) {
    }

    record GatewayWebhookResult(
            PaymentStatus status,
            String providerTransactionId,
            String failureReason
    ) {
    }
}
