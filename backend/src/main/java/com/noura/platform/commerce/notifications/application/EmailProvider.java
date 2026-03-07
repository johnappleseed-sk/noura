package com.noura.platform.commerce.notifications.application;

/**
 * Abstract interface for email sending providers.
 * Implement this interface for each email provider (SendGrid, SES, SMTP).
 */
public interface EmailProvider {

    /**
     * Get the provider identifier (e.g., "sendgrid", "ses", "smtp").
     */
    String getProviderId();

    /**
     * Check if this provider is enabled and configured.
     */
    boolean isEnabled();

    /**
     * Get the priority of this provider (lower = higher priority).
     */
    int getPriority();

    /**
     * Send an email.
     *
     * @param request Email send request
     * @return Result of the send operation
     */
    SendResult send(SendEmailRequest request);

    // ===============================
    // Request/Response Records
    // ===============================

    record SendEmailRequest(
            String fromEmail,
            String fromName,
            String toEmail,
            String toName,
            String subject,
            String textBody,
            String htmlBody,
            String replyTo
    ) {}

    record SendResult(
            boolean success,
            String messageId,
            String errorCode,
            String errorMessage
    ) {
        public static SendResult success(String messageId) {
            return new SendResult(true, messageId, null, null);
        }

        public static SendResult failure(String errorCode, String errorMessage) {
            return new SendResult(false, null, errorCode, errorMessage);
        }
    }
}
