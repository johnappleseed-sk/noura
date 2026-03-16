package com.noura.platform.service.payment;

import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.domain.enums.PaymentStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MockPaymentGateway implements PaymentGateway {

    public static final String PROVIDER_CODE = "mock";

    @Override
    public String providerCode() {
        return PROVIDER_CODE;
    }

    @Override
    public GatewayCreateResult createPayment(GatewayCreateRequest request) {
        String providerTransactionId = "mock_txn_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return new GatewayCreateResult(PaymentStatus.PENDING, providerTransactionId, null);
    }

    @Override
    public GatewayWebhookResult processWebhook(GatewayWebhookRequest request) {
        if (request.status() != PaymentStatus.SUCCESS && request.status() != PaymentStatus.FAILED) {
            throw new BadRequestException("PAYMENT_WEBHOOK_STATUS_INVALID", "Mock webhook status must be SUCCESS or FAILED");
        }

        String providerTransactionId = request.providerTransactionId();
        if (providerTransactionId == null || providerTransactionId.isBlank()) {
            providerTransactionId = "mock_txn_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }

        String failureReason = request.failureReason();
        if (request.status() == PaymentStatus.FAILED && (failureReason == null || failureReason.isBlank())) {
            failureReason = "Mock payment failure";
        }

        return new GatewayWebhookResult(request.status(), providerTransactionId, failureReason);
    }
}
