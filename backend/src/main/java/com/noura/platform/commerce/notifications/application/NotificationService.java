package com.noura.platform.commerce.notifications.application;

import com.noura.platform.commerce.notifications.domain.NotificationChannel;
import com.noura.platform.commerce.notifications.domain.NotificationLog;
import com.noura.platform.commerce.notifications.domain.NotificationStatus;
import com.noura.platform.commerce.notifications.domain.NotificationType;
import com.noura.platform.commerce.notifications.infrastructure.NotificationLogRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Core notification service for sending emails, SMS, and other notifications.
 * Uses EmailProviderRegistry for real email delivery via SendGrid, SMTP, etc.
 */
@Service
@Transactional
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final int MAX_RETRY_COUNT = 3;
    private static final String REFERENCE_TYPE_ORDER = "ORDER";
    private static final String REFERENCE_TYPE_CUSTOMER = "CUSTOMER";
    private static final String REFERENCE_TYPE_RETURN = "RETURN";

    private final NotificationLogRepo notificationLogRepo;
    private final EmailProviderRegistry emailProviderRegistry;
    private final boolean emailEnabled;
    private final String fromAddress;
    private final String fromName;

    public NotificationService(NotificationLogRepo notificationLogRepo,
                              EmailProviderRegistry emailProviderRegistry,
                              @Value("${app.notifications.email.enabled:false}") boolean emailEnabled,
                              @Value("${app.notifications.email.from-address:noreply@example.com}") String fromAddress,
                              @Value("${app.notifications.email.from-name:POS System}") String fromName) {
        this.notificationLogRepo = notificationLogRepo;
        this.emailProviderRegistry = emailProviderRegistry;
        this.emailEnabled = emailEnabled;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
    }

    // ===============================
    // Order Notifications
    // ===============================

    public void sendOrderConfirmation(Long orderId, String customerEmail, String orderNumber, String orderTotal) {
        if (!emailEnabled) {
            log.info("Email disabled - skipping order confirmation for order {} to {}", orderId, customerEmail);
            return;
        }

        String subject = "Order Confirmation - " + orderNumber;
        String body = buildOrderConfirmationBody(orderNumber, orderTotal);

        sendEmail(customerEmail, subject, body, NotificationType.ORDER_CONFIRMATION, REFERENCE_TYPE_ORDER, orderId);
    }

    public void sendOrderShipped(Long orderId, String customerEmail, String orderNumber, String trackingNumber, String carrier) {
        if (!emailEnabled) {
            log.info("Email disabled - skipping shipping notification for order {} to {}", orderId, customerEmail);
            return;
        }

        String subject = "Your Order Has Shipped - " + orderNumber;
        String body = buildOrderShippedBody(orderNumber, trackingNumber, carrier);

        sendEmail(customerEmail, subject, body, NotificationType.ORDER_SHIPPED, REFERENCE_TYPE_ORDER, orderId);
    }

    public void sendOrderDelivered(Long orderId, String customerEmail, String orderNumber) {
        if (!emailEnabled) {
            log.info("Email disabled - skipping delivery notification for order {} to {}", orderId, customerEmail);
            return;
        }

        String subject = "Your Order Has Been Delivered - " + orderNumber;
        String body = buildOrderDeliveredBody(orderNumber);

        sendEmail(customerEmail, subject, body, NotificationType.ORDER_DELIVERED, REFERENCE_TYPE_ORDER, orderId);
    }

    public void sendOrderCancelled(Long orderId, String customerEmail, String orderNumber, String reason) {
        if (!emailEnabled) {
            log.info("Email disabled - skipping cancellation notification for order {} to {}", orderId, customerEmail);
            return;
        }

        String subject = "Order Cancelled - " + orderNumber;
        String body = buildOrderCancelledBody(orderNumber, reason);

        sendEmail(customerEmail, subject, body, NotificationType.ORDER_CANCELLED, REFERENCE_TYPE_ORDER, orderId);
    }

    public void sendOrderRefunded(Long orderId, String customerEmail, String orderNumber, String refundAmount) {
        if (!emailEnabled) {
            log.info("Email disabled - skipping refund notification for order {} to {}", orderId, customerEmail);
            return;
        }

        String subject = "Refund Processed - " + orderNumber;
        String body = buildOrderRefundedBody(orderNumber, refundAmount);

        sendEmail(customerEmail, subject, body, NotificationType.ORDER_REFUNDED, REFERENCE_TYPE_ORDER, orderId);
    }

    // ===============================
    // Customer Account Notifications
    // ===============================

    public void sendWelcomeEmail(Long customerId, String customerEmail, String firstName) {
        if (!emailEnabled) {
            log.info("Email disabled - skipping welcome email for customer {} to {}", customerId, customerEmail);
            return;
        }

        String subject = "Welcome to Our Store!";
        String body = buildWelcomeBody(firstName);

        sendEmail(customerEmail, subject, body, NotificationType.CUSTOMER_WELCOME, REFERENCE_TYPE_CUSTOMER, customerId);
    }

    public void sendPasswordResetEmail(Long customerId, String customerEmail, String resetToken) {
        if (!emailEnabled) {
            log.info("Email disabled - skipping password reset for customer {} to {}", customerId, customerEmail);
            return;
        }

        String subject = "Password Reset Request";
        String body = buildPasswordResetBody(resetToken);

        sendEmail(customerEmail, subject, body, NotificationType.CUSTOMER_PASSWORD_RESET, REFERENCE_TYPE_CUSTOMER, customerId);
    }

    public void sendEmailVerification(Long customerId, String customerEmail, String verificationToken) {
        if (!emailEnabled) {
            log.info("Email disabled - skipping email verification for customer {} to {}", customerId, customerEmail);
            return;
        }

        String subject = "Verify Your Email Address";
        String body = buildEmailVerificationBody(verificationToken);

        sendEmail(customerEmail, subject, body, NotificationType.CUSTOMER_EMAIL_VERIFICATION, REFERENCE_TYPE_CUSTOMER, customerId);
    }

    // ===============================
    // Return Notifications
    // ===============================

    public void sendReturnRequestReceived(Long returnId, String customerEmail, String returnNumber) {
        if (!emailEnabled) {
            log.info("Email disabled - skipping return request notification for return {} to {}", returnId, customerEmail);
            return;
        }

        String subject = "Return Request Received - " + returnNumber;
        String body = buildReturnRequestReceivedBody(returnNumber);

        sendEmail(customerEmail, subject, body, NotificationType.RETURN_REQUEST_RECEIVED, REFERENCE_TYPE_RETURN, returnId);
    }

    public void sendReturnApproved(Long returnId, String customerEmail, String returnNumber, String instructions) {
        if (!emailEnabled) {
            log.info("Email disabled - skipping return approval notification for return {} to {}", returnId, customerEmail);
            return;
        }

        String subject = "Return Approved - " + returnNumber;
        String body = buildReturnApprovedBody(returnNumber, instructions);

        sendEmail(customerEmail, subject, body, NotificationType.RETURN_APPROVED, REFERENCE_TYPE_RETURN, returnId);
    }

    public void sendReturnRejected(Long returnId, String customerEmail, String returnNumber, String reason) {
        if (!emailEnabled) {
            log.info("Email disabled - skipping return rejection notification for return {} to {}", returnId, customerEmail);
            return;
        }

        String subject = "Return Request Update - " + returnNumber;
        String body = buildReturnRejectedBody(returnNumber, reason);

        sendEmail(customerEmail, subject, body, NotificationType.RETURN_REJECTED, REFERENCE_TYPE_RETURN, returnId);
    }

    // ===============================
    // Core Send Methods
    // ===============================

    /**
     * General-purpose notification (e.g. B2B reminders, system alerts).
     */
    public void sendNotification(String recipient, String subject, String body) {
        if (!emailEnabled) {
            log.info("Email disabled - skipping notification to {}: {}", recipient, subject);
            return;
        }
        sendEmail(recipient, subject, body, NotificationType.SYSTEM_ALERT, null, null);
    }

    private void sendEmail(String recipient, String subject, String body,
                          NotificationType type, String referenceType, Long referenceId) {
        NotificationLog notification = new NotificationLog();
        notification.setChannel(NotificationChannel.EMAIL);
        notification.setType(type);
        notification.setRecipient(recipient);
        notification.setSubject(subject);
        notification.setBody(body);
        notification.setReferenceType(referenceType);
        notification.setReferenceId(referenceId);
        notification.setStatus(NotificationStatus.PENDING);

        try {
            log.info("Sending email to {}: {}", recipient, subject);

            // Use the email provider registry to send via configured provider
            EmailProvider.SendEmailRequest emailRequest = new EmailProvider.SendEmailRequest(
                    fromAddress,
                    fromName,
                    recipient,
                    null, // toName
                    subject,
                    body,
                    null, // htmlBody - could generate HTML version
                    null  // replyTo
            );

            EmailProvider.SendResult result = emailProviderRegistry.sendWithFallback(emailRequest);

            if (result.success()) {
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
                log.info("Email sent successfully to {} (messageId: {})", recipient, result.messageId());
            } else {
                notification.setStatus(NotificationStatus.FAILED);
                notification.setFailureReason(result.errorCode() + ": " + result.errorMessage());
                log.warn("Email send failed to {}: {}", recipient, result.errorMessage());
            }
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", recipient, e.getMessage());
            notification.setStatus(NotificationStatus.FAILED);
            notification.setFailureReason(truncate(e.getMessage(), 500));
        }

        notificationLogRepo.save(notification);
    }

    // ===============================
    // Retry Failed Notifications
    // ===============================

    public List<NotificationLog> retryFailedNotifications() {
        List<NotificationLog> failed = notificationLogRepo
                .findByStatusAndRetryCountLessThanOrderByCreatedAtAsc(NotificationStatus.FAILED, MAX_RETRY_COUNT);

        for (NotificationLog notification : failed) {
            try {
                log.info("Retrying notification {} (attempt {})", notification.getId(), notification.getRetryCount() + 1);
                notification.setRetryCount(notification.getRetryCount() + 1);

                EmailProvider.SendEmailRequest emailRequest = new EmailProvider.SendEmailRequest(
                        fromAddress,
                        fromName,
                        notification.getRecipient(),
                        null,
                        notification.getSubject(),
                        notification.getBody(),
                        null,
                        null
                );

                EmailProvider.SendResult result = emailProviderRegistry.sendWithFallback(emailRequest);

                if (result.success()) {
                    notification.setStatus(NotificationStatus.SENT);
                    notification.setSentAt(LocalDateTime.now());
                    notification.setFailureReason(null);
                } else {
                    notification.setFailureReason(result.errorCode() + ": " + result.errorMessage());
                }
            } catch (Exception e) {
                notification.setFailureReason(truncate(e.getMessage(), 500));
            }
        }

        return failed;
    }

    // ===============================
    // Query Methods
    // ===============================

    @Transactional(readOnly = true)
    public List<NotificationLog> getNotificationsForOrder(Long orderId) {
        return notificationLogRepo.findByReferenceTypeAndReferenceIdOrderByCreatedAtDesc(REFERENCE_TYPE_ORDER, orderId);
    }

    @Transactional(readOnly = true)
    public List<NotificationLog> getNotificationsForCustomer(Long customerId) {
        return notificationLogRepo.findByReferenceTypeAndReferenceIdOrderByCreatedAtDesc(REFERENCE_TYPE_CUSTOMER, customerId);
    }

    @Transactional(readOnly = true)
    public List<NotificationLog> getNotificationsByRecipient(String recipient) {
        return notificationLogRepo.findByRecipientOrderByCreatedAtDesc(recipient);
    }

    // ===============================
    // Email Body Builders
    // ===============================

    private String buildOrderConfirmationBody(String orderNumber, String orderTotal) {
        return String.format("""
            Thank you for your order!
            
            Order Number: %s
            Order Total: %s
            
            We'll send you a shipping notification once your order is on its way.
            
            Thank you for shopping with us!
            """, orderNumber, orderTotal);
    }

    private String buildOrderShippedBody(String orderNumber, String trackingNumber, String carrier) {
        String trackingInfo = trackingNumber != null
                ? String.format("Tracking Number: %s\nCarrier: %s", trackingNumber, carrier != null ? carrier : "N/A")
                : "Tracking information will be updated shortly.";

        return String.format("""
            Great news! Your order has shipped.
            
            Order Number: %s
            %s
            
            Thank you for shopping with us!
            """, orderNumber, trackingInfo);
    }

    private String buildOrderDeliveredBody(String orderNumber) {
        return String.format("""
            Your order has been delivered!
            
            Order Number: %s
            
            We hope you enjoy your purchase. If you have any questions, please contact our support team.
            
            Thank you for shopping with us!
            """, orderNumber);
    }

    private String buildOrderCancelledBody(String orderNumber, String reason) {
        String reasonText = reason != null && !reason.isBlank()
                ? "Reason: " + reason
                : "";

        return String.format("""
            Your order has been cancelled.
            
            Order Number: %s
            %s
            
            If you have any questions, please contact our support team.
            """, orderNumber, reasonText);
    }

    private String buildOrderRefundedBody(String orderNumber, String refundAmount) {
        return String.format("""
            Your refund has been processed.
            
            Order Number: %s
            Refund Amount: %s
            
            The refund should appear in your account within 5-10 business days.
            
            If you have any questions, please contact our support team.
            """, orderNumber, refundAmount);
    }

    private String buildWelcomeBody(String firstName) {
        String greeting = firstName != null && !firstName.isBlank()
                ? "Hi " + firstName + ","
                : "Welcome!";

        return String.format("""
            %s
            
            Thank you for creating an account with us!
            
            You can now:
            - Track your orders
            - Save your shipping addresses
            - Get personalized recommendations
            
            Happy shopping!
            """, greeting);
    }

    private String buildPasswordResetBody(String resetToken) {
        return String.format("""
            You requested a password reset.
            
            Use this code to reset your password: %s
            
            This code will expire in 30 minutes.
            
            If you didn't request this, please ignore this email.
            """, resetToken);
    }

    private String buildEmailVerificationBody(String verificationToken) {
        return String.format("""
            Please verify your email address.
            
            Use this code to verify your email: %s
            
            This code will expire in 24 hours.
            """, verificationToken);
    }

    private String buildReturnRequestReceivedBody(String returnNumber) {
        return String.format("""
            We've received your return request.
            
            Return Number: %s
            
            We'll review your request and get back to you within 1-2 business days.
            """, returnNumber);
    }

    private String buildReturnApprovedBody(String returnNumber, String instructions) {
        String instructionsText = instructions != null && !instructions.isBlank()
                ? "\nReturn Instructions:\n" + instructions
                : "";

        return String.format("""
            Your return request has been approved!
            
            Return Number: %s
            %s
            
            Please ship the item(s) within 14 days.
            """, returnNumber, instructionsText);
    }

    private String buildReturnRejectedBody(String returnNumber, String reason) {
        String reasonText = reason != null && !reason.isBlank()
                ? "Reason: " + reason
                : "";

        return String.format("""
            We're sorry, but we couldn't approve your return request.
            
            Return Number: %s
            %s
            
            If you have any questions, please contact our support team.
            """, returnNumber, reasonText);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
