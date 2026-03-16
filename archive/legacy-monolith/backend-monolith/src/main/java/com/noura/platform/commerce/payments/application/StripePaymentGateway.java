package com.noura.platform.commerce.payments.application;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCancelParams;
import com.stripe.param.PaymentIntentCaptureParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Stripe payment gateway implementation.
 * Handles payment intents, captures, refunds, and voids.
 */
@Component
public class StripePaymentGateway implements PaymentGateway {
    private static final Logger log = LoggerFactory.getLogger(StripePaymentGateway.class);
    private static final String PROVIDER_ID = "stripe";

    private final boolean enabled;
    private final String secretKey;

    public StripePaymentGateway(
            @Value("${app.payments.stripe.enabled:false}") boolean enabled,
            @Value("${app.payments.stripe.secret-key:}") String secretKey) {
        this.enabled = enabled;
        this.secretKey = secretKey;

        if (enabled && secretKey != null && !secretKey.isBlank()) {
            Stripe.apiKey = secretKey;
            log.info("Stripe payment gateway initialized");
        }
    }

    @Override
    public String getProviderId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean isEnabled() {
        return enabled && secretKey != null && !secretKey.isBlank();
    }

    @Override
    public PaymentResult createPayment(CreatePaymentRequest request) {
        if (!isEnabled()) {
            return PaymentResult.failure("NOT_CONFIGURED", "Stripe is not configured");
        }

        try {
            // Convert amount to cents (Stripe uses smallest currency unit)
            long amountInCents = request.amount()
                    .multiply(BigDecimal.valueOf(100))
                    .longValue();

            PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(request.currencyCode().toLowerCase())
                    .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL) // Authorize only
                    .putMetadata("order_id", request.orderId());

            if (request.description() != null && !request.description().isBlank()) {
                paramsBuilder.setDescription(request.description());
            }

            if (request.customerEmail() != null && !request.customerEmail().isBlank()) {
                paramsBuilder.setReceiptEmail(request.customerEmail());
            }

            PaymentIntent intent = PaymentIntent.create(paramsBuilder.build());

            log.info("Created Stripe PaymentIntent {} for order {} ({} {})",
                    intent.getId(), request.orderId(), request.currencyCode(), request.amount());

            PaymentStatus status = mapStripeStatus(intent.getStatus());

            return PaymentResult.success(intent.getId(), status, intent.toJson());

        } catch (StripeException e) {
            log.error("Stripe createPayment failed: {}", e.getMessage());
            return PaymentResult.failure(e.getCode(), e.getMessage());
        }
    }

    @Override
    public PaymentResult capturePayment(String providerReference, BigDecimal amount) {
        if (!isEnabled()) {
            return PaymentResult.failure("NOT_CONFIGURED", "Stripe is not configured");
        }

        try {
            PaymentIntent intent = PaymentIntent.retrieve(providerReference);

            PaymentIntentCaptureParams.Builder paramsBuilder = PaymentIntentCaptureParams.builder();

            // If amount specified and different from original, capture partial
            if (amount != null) {
                long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();
                paramsBuilder.setAmountToCapture(amountInCents);
            }

            PaymentIntent captured = intent.capture(paramsBuilder.build());

            log.info("Captured Stripe PaymentIntent {}", captured.getId());

            return PaymentResult.success(captured.getId(), PaymentStatus.CAPTURED, captured.toJson());

        } catch (StripeException e) {
            log.error("Stripe capturePayment failed for {}: {}", providerReference, e.getMessage());
            return PaymentResult.failure(e.getCode(), e.getMessage());
        }
    }

    @Override
    public PaymentResult refundPayment(String providerReference, BigDecimal amount, String reason) {
        if (!isEnabled()) {
            return PaymentResult.failure("NOT_CONFIGURED", "Stripe is not configured");
        }

        try {
            RefundCreateParams.Builder paramsBuilder = RefundCreateParams.builder()
                    .setPaymentIntent(providerReference);

            if (amount != null) {
                long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();
                paramsBuilder.setAmount(amountInCents);
            }

            if (reason != null && !reason.isBlank()) {
                paramsBuilder.putMetadata("reason", reason);
                // Map to Stripe's reason enum if applicable
                paramsBuilder.setReason(mapRefundReason(reason));
            }

            Refund refund = Refund.create(paramsBuilder.build());

            log.info("Created Stripe Refund {} for PaymentIntent {}", refund.getId(), providerReference);

            return PaymentResult.success(refund.getId(), PaymentStatus.REFUNDED, refund.toJson());

        } catch (StripeException e) {
            log.error("Stripe refundPayment failed for {}: {}", providerReference, e.getMessage());
            return PaymentResult.failure(e.getCode(), e.getMessage());
        }
    }

    @Override
    public PaymentResult voidPayment(String providerReference) {
        if (!isEnabled()) {
            return PaymentResult.failure("NOT_CONFIGURED", "Stripe is not configured");
        }

        try {
            PaymentIntent intent = PaymentIntent.retrieve(providerReference);
            PaymentIntent cancelled = intent.cancel(PaymentIntentCancelParams.builder().build());

            log.info("Cancelled Stripe PaymentIntent {}", cancelled.getId());

            return PaymentResult.success(cancelled.getId(), PaymentStatus.VOIDED, cancelled.toJson());

        } catch (StripeException e) {
            log.error("Stripe voidPayment failed for {}: {}", providerReference, e.getMessage());
            return PaymentResult.failure(e.getCode(), e.getMessage());
        }
    }

    @Override
    public PaymentStatus getPaymentStatus(String providerReference) {
        if (!isEnabled()) {
            return PaymentStatus.UNKNOWN;
        }

        try {
            PaymentIntent intent = PaymentIntent.retrieve(providerReference);
            return mapStripeStatus(intent.getStatus());

        } catch (StripeException e) {
            log.error("Stripe getPaymentStatus failed for {}: {}", providerReference, e.getMessage());
            return PaymentStatus.UNKNOWN;
        }
    }

    private PaymentStatus mapStripeStatus(String stripeStatus) {
        if (stripeStatus == null) {
            return PaymentStatus.UNKNOWN;
        }
        return switch (stripeStatus) {
            case "requires_payment_method", "requires_confirmation" -> PaymentStatus.PENDING;
            case "requires_action" -> PaymentStatus.REQUIRES_ACTION;
            case "processing" -> PaymentStatus.PENDING;
            case "requires_capture" -> PaymentStatus.AUTHORIZED;
            case "succeeded" -> PaymentStatus.CAPTURED;
            case "canceled" -> PaymentStatus.VOIDED;
            default -> PaymentStatus.UNKNOWN;
        };
    }

    private RefundCreateParams.Reason mapRefundReason(String reason) {
        if (reason == null) {
            return null;
        }
        String lowerReason = reason.toLowerCase();
        if (lowerReason.contains("duplicate")) {
            return RefundCreateParams.Reason.DUPLICATE;
        }
        if (lowerReason.contains("fraud")) {
            return RefundCreateParams.Reason.FRAUDULENT;
        }
        return RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER;
    }
}
