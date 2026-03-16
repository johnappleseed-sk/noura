package com.noura.platform.commerce.notifications.application;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * SMTP email provider implementation using Spring Mail.
 * Acts as a fallback when SendGrid/SES are not available.
 */
@Component
public class SmtpEmailProvider implements EmailProvider {
    private static final Logger log = LoggerFactory.getLogger(SmtpEmailProvider.class);
    private static final String PROVIDER_ID = "smtp";

    private final boolean enabled;
    private final JavaMailSender mailSender;

    public SmtpEmailProvider(
            @Value("${app.notifications.smtp.enabled:false}") boolean enabled,
            JavaMailSender mailSender) {
        this.enabled = enabled;
        this.mailSender = mailSender;

        if (enabled) {
            log.info("SMTP email provider initialized");
        }
    }

    @Override
    public String getProviderId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean isEnabled() {
        return enabled && mailSender != null;
    }

    @Override
    public int getPriority() {
        return 100; // Low priority - use as fallback
    }

    @Override
    public SendResult send(SendEmailRequest request) {
        if (!isEnabled()) {
            return SendResult.failure("NOT_CONFIGURED", "SMTP is not configured");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Set from
            if (request.fromName() != null && !request.fromName().isBlank()) {
                helper.setFrom(request.fromEmail(), request.fromName());
            } else {
                helper.setFrom(request.fromEmail());
            }

            // Set to
            if (request.toName() != null && !request.toName().isBlank()) {
                helper.setTo(String.format("%s <%s>", request.toName(), request.toEmail()));
            } else {
                helper.setTo(request.toEmail());
            }

            helper.setSubject(request.subject());

            // Set body (prefer HTML if available)
            if (request.htmlBody() != null && !request.htmlBody().isBlank()) {
                String textBody = request.textBody() != null ? request.textBody() : "";
                helper.setText(textBody, request.htmlBody());
            } else if (request.textBody() != null) {
                helper.setText(request.textBody());
            }

            // Set reply-to
            if (request.replyTo() != null && !request.replyTo().isBlank()) {
                helper.setReplyTo(request.replyTo());
            }

            mailSender.send(message);

            String messageId = "smtp_" + UUID.randomUUID().toString().substring(0, 12);
            log.info("SMTP email sent to {} (messageId: {})", request.toEmail(), messageId);
            return SendResult.success(messageId);

        } catch (MessagingException e) {
            log.error("SMTP messaging error: {}", e.getMessage());
            return SendResult.failure("MESSAGING_ERROR", e.getMessage());
        } catch (MailException e) {
            log.error("SMTP mail error: {}", e.getMessage());
            return SendResult.failure("MAIL_ERROR", e.getMessage());
        } catch (Exception e) {
            log.error("SMTP send failed: {}", e.getMessage());
            return SendResult.failure("UNKNOWN_ERROR", e.getMessage());
        }
    }
}
