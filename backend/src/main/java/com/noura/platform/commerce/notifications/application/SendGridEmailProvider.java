package com.noura.platform.commerce.notifications.application;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * SendGrid email provider implementation.
 */
@Component
public class SendGridEmailProvider implements EmailProvider {
    private static final Logger log = LoggerFactory.getLogger(SendGridEmailProvider.class);
    private static final String PROVIDER_ID = "sendgrid";

    private final boolean enabled;
    private final String apiKey;
    private final SendGrid sendGrid;

    public SendGridEmailProvider(
            @Value("${app.notifications.sendgrid.enabled:false}") boolean enabled,
            @Value("${app.notifications.sendgrid.api-key:}") String apiKey) {
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.sendGrid = (enabled && apiKey != null && !apiKey.isBlank())
                ? new SendGrid(apiKey)
                : null;

        if (isEnabled()) {
            log.info("SendGrid email provider initialized");
        }
    }

    @Override
    public String getProviderId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean isEnabled() {
        return enabled && apiKey != null && !apiKey.isBlank() && sendGrid != null;
    }

    @Override
    public int getPriority() {
        return 10; // High priority - prefer SendGrid when available
    }

    @Override
    public SendResult send(SendEmailRequest request) {
        if (!isEnabled()) {
            return SendResult.failure("NOT_CONFIGURED", "SendGrid is not configured");
        }

        try {
            Email from = new Email(request.fromEmail(), request.fromName());
            Email to = new Email(request.toEmail(), request.toName());

            Mail mail = new Mail();
            mail.setFrom(from);
            mail.setSubject(request.subject());

            Personalization personalization = new Personalization();
            personalization.addTo(to);
            mail.addPersonalization(personalization);

            // Add text content
            if (request.textBody() != null && !request.textBody().isBlank()) {
                mail.addContent(new Content("text/plain", request.textBody()));
            }

            // Add HTML content
            if (request.htmlBody() != null && !request.htmlBody().isBlank()) {
                mail.addContent(new Content("text/html", request.htmlBody()));
            }

            // Add reply-to if specified
            if (request.replyTo() != null && !request.replyTo().isBlank()) {
                mail.setReplyTo(new Email(request.replyTo()));
            }

            Request sendGridRequest = new Request();
            sendGridRequest.setMethod(Method.POST);
            sendGridRequest.setEndpoint("mail/send");
            sendGridRequest.setBody(mail.build());

            Response response = sendGrid.api(sendGridRequest);

            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                String messageId = response.getHeaders().get("X-Message-Id");
                log.info("SendGrid email sent to {} (messageId: {})", request.toEmail(), messageId);
                return SendResult.success(messageId);
            } else {
                log.error("SendGrid failed with status {}: {}", response.getStatusCode(), response.getBody());
                return SendResult.failure(
                        "HTTP_" + response.getStatusCode(),
                        response.getBody()
                );
            }

        } catch (IOException e) {
            log.error("SendGrid send failed: {}", e.getMessage());
            return SendResult.failure("IO_ERROR", e.getMessage());
        }
    }
}
