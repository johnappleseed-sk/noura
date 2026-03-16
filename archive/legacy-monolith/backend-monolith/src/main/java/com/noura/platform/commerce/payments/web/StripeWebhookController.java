package com.noura.platform.commerce.payments.web;

import com.noura.platform.commerce.notifications.application.NotificationService;
import com.noura.platform.commerce.orders.domain.Order;
import com.noura.platform.commerce.orders.domain.OrderStatus;
import com.noura.platform.commerce.orders.infrastructure.OrderRepo;
import com.noura.platform.commerce.payments.domain.PaymentTransaction;
import com.noura.platform.commerce.payments.domain.PaymentTransactionStatus;
import com.noura.platform.commerce.payments.infrastructure.PaymentTransactionRepo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Stripe webhook handler for payment events.
 * Handles payment_intent.succeeded, payment_intent.payment_failed,
 * charge.refunded, and other Stripe events.
 */
@RestController
@RequestMapping("/api/webhooks/stripe")
public class StripeWebhookController {
    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final String webhookSecret;
    private final PaymentTransactionRepo paymentRepo;
    private final OrderRepo orderRepo;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public StripeWebhookController(
            @Value("${app.payments.stripe.webhook-secret:}") String webhookSecret,
            PaymentTransactionRepo paymentRepo,
            OrderRepo orderRepo,
            NotificationService notificationService,
            ObjectMapper objectMapper) {
        this.webhookSecret = webhookSecret;
        this.paymentRepo = paymentRepo;
        this.orderRepo = orderRepo;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {

        Event event;

        // Verify webhook signature if secret is configured
        if (webhookSecret != null && !webhookSecret.isBlank()) {
            try {
                event = Webhook.constructEvent(payload, signature, webhookSecret);
            } catch (SignatureVerificationException e) {
                log.warn("Stripe webhook signature verification failed: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid signature"));
            }
        } else {
            // Development mode - parse without verification
            log.warn("Stripe webhook secret not configured - skipping signature verification");
            event = Event.GSON.fromJson(payload, Event.class);
        }

        log.info("Received Stripe webhook: {}", event.getType());

        try {
            switch (event.getType()) {
                case "payment_intent.succeeded" -> handlePaymentSucceeded(event);
                case "payment_intent.payment_failed" -> handlePaymentFailed(event);
                case "payment_intent.canceled" -> handlePaymentCanceled(event);
                case "charge.refunded" -> handleChargeRefunded(event);
                case "charge.dispute.created" -> handleDisputeCreated(event);
                default -> log.debug("Unhandled Stripe event type: {}", event.getType());
            }

            return ResponseEntity.ok(Map.of("status", "received"));

        } catch (Exception e) {
            log.error("Error processing Stripe webhook {}: {}", event.getType(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Processing failed"));
        }
    }

    private void handlePaymentSucceeded(Event event) {
        PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer()
                .getObject().orElse(null);

        if (intent == null) {
            log.warn("Could not deserialize PaymentIntent from event");
            return;
        }

        String paymentIntentId = intent.getId();
        String orderId = intent.getMetadata().get("order_id");

        log.info("Payment succeeded: {} for order {}", paymentIntentId, orderId);

        // Update payment record
        Optional<PaymentTransaction> paymentOpt = paymentRepo.findByProviderReference(paymentIntentId);
        if (paymentOpt.isPresent()) {
            PaymentTransaction payment = paymentOpt.get();
            payment.setStatus(PaymentTransactionStatus.CAPTURED);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepo.save(payment);

            // Update order status
            Order order = payment.getOrder();
            if (order != null && order.getStatus() == OrderStatus.PENDING_PAYMENT) {
                order.setStatus(OrderStatus.CONFIRMED);
                order.setUpdatedAt(LocalDateTime.now());
                orderRepo.save(order);

                // Send confirmation notification
                if (order.getCustomerEmail() != null) {
                    notificationService.sendOrderConfirmation(
                            order.getId(), order.getCustomerEmail(),
                            order.getOrderNumber(), payment.getAmount().toPlainString());
                }
            }
        } else {
            log.warn("No payment found for PaymentIntent: {}", paymentIntentId);
        }
    }

    private void handlePaymentFailed(Event event) {
        PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer()
                .getObject().orElse(null);

        if (intent == null) return;

        String paymentIntentId = intent.getId();
        String orderId = intent.getMetadata().get("order_id");
        String failureMessage = intent.getLastPaymentError() != null
                ? intent.getLastPaymentError().getMessage()
                : "Payment failed";

        log.warn("Payment failed: {} for order {} - {}", paymentIntentId, orderId, failureMessage);

        Optional<PaymentTransaction> paymentOpt = paymentRepo.findByProviderReference(paymentIntentId);
        if (paymentOpt.isPresent()) {
            PaymentTransaction payment = paymentOpt.get();
            payment.setStatus(PaymentTransactionStatus.FAILED);
            payment.setFailureReason(failureMessage);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepo.save(payment);

            // Update order status
            Order order = payment.getOrder();
            if (order != null) {
                order.setStatus(OrderStatus.PAYMENT_FAILED);
                order.setUpdatedAt(LocalDateTime.now());
                orderRepo.save(order);

                // Send payment failed notification
                if (order.getCustomerEmail() != null) {
                    notificationService.sendOrderCancelled(
                            order.getId(), order.getCustomerEmail(),
                            order.getOrderNumber(), failureMessage);
                }
            }
        }
    }

    private void handlePaymentCanceled(Event event) {
        PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer()
                .getObject().orElse(null);

        if (intent == null) return;

        String paymentIntentId = intent.getId();
        log.info("Payment canceled: {}", paymentIntentId);

        Optional<PaymentTransaction> paymentOpt = paymentRepo.findByProviderReference(paymentIntentId);
        if (paymentOpt.isPresent()) {
            PaymentTransaction payment = paymentOpt.get();
            payment.setStatus(PaymentTransactionStatus.VOIDED);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepo.save(payment);
        }
    }

    private void handleChargeRefunded(Event event) {
        try {
            JsonNode data = objectMapper.readTree(event.toJson()).path("data").path("object");
            String paymentIntentId = data.path("payment_intent").asText();
            long amountRefunded = data.path("amount_refunded").asLong();
            long amountTotal = data.path("amount").asLong();

            log.info("Charge refunded: {} - refunded {} of {}", paymentIntentId, amountRefunded, amountTotal);

            Optional<PaymentTransaction> paymentOpt = paymentRepo.findByProviderReference(paymentIntentId);
            if (paymentOpt.isPresent()) {
                PaymentTransaction payment = paymentOpt.get();

                if (amountRefunded >= amountTotal) {
                    payment.setStatus(PaymentTransactionStatus.REFUNDED);
                } else {
                    payment.setStatus(PaymentTransactionStatus.PARTIALLY_REFUNDED);
                }
                payment.setUpdatedAt(LocalDateTime.now());
                paymentRepo.save(payment);

                // Send refund notification
                Order order = payment.getOrder();
                if (order != null && order.getCustomerEmail() != null) {
                    String refundAmount = String.format("%.2f", amountRefunded / 100.0);
                    notificationService.sendOrderRefunded(
                            order.getId(), order.getCustomerEmail(),
                            order.getOrderNumber(), refundAmount);
                }
            }
        } catch (Exception e) {
            log.error("Error handling charge.refunded event: {}", e.getMessage());
        }
    }

    private void handleDisputeCreated(Event event) {
        try {
            JsonNode data = objectMapper.readTree(event.toJson()).path("data").path("object");
            String chargeId = data.path("charge").asText();
            String reason = data.path("reason").asText();
            long amount = data.path("amount").asLong();

            log.warn("Dispute created for charge {}: {} (amount: {})", chargeId, reason, amount);

            // TODO: Create dispute record and alert admin
            // This would typically trigger an admin notification and create a dispute entity

        } catch (Exception e) {
            log.error("Error handling dispute.created event: {}", e.getMessage());
        }
    }
}
