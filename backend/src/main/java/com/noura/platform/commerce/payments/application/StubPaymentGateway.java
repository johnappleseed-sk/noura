package com.noura.platform.commerce.payments.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Stub payment gateway for development and testing.
 * Always succeeds immediately - replace with real provider implementations in production.
 */
@Component
public class StubPaymentGateway implements PaymentGateway {
    private static final Logger log = LoggerFactory.getLogger(StubPaymentGateway.class);
    private static final String PROVIDER_ID = "stub";

    @Override
    public String getProviderId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean isEnabled() {
        return true; // Always enabled for development
    }

    @Override
    public PaymentResult createPayment(CreatePaymentRequest request) {
        log.info("STUB: Creating payment for {} {} (order: {})",
                request.currencyCode(), request.amount(), request.orderId());

        String reference = "stub_" + UUID.randomUUID().toString().substring(0, 12);

        return PaymentResult.success(reference, PaymentStatus.AUTHORIZED,
                "Stub payment authorized: " + reference);
    }

    @Override
    public PaymentResult capturePayment(String providerReference, BigDecimal amount) {
        log.info("STUB: Capturing payment {} for {}", providerReference, amount);

        return PaymentResult.success(providerReference, PaymentStatus.CAPTURED,
                "Stub payment captured: " + providerReference);
    }

    @Override
    public PaymentResult refundPayment(String providerReference, BigDecimal amount, String reason) {
        log.info("STUB: Refunding payment {} for {} (reason: {})", providerReference, amount, reason);

        return PaymentResult.success(providerReference, PaymentStatus.REFUNDED,
                "Stub payment refunded: " + providerReference);
    }

    @Override
    public PaymentResult voidPayment(String providerReference) {
        log.info("STUB: Voiding payment {}", providerReference);

        return PaymentResult.success(providerReference, PaymentStatus.VOIDED,
                "Stub payment voided: " + providerReference);
    }

    @Override
    public PaymentStatus getPaymentStatus(String providerReference) {
        log.info("STUB: Getting status for payment {}", providerReference);

        // Stub always returns captured
        return PaymentStatus.CAPTURED;
    }
}
