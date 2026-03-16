package com.noura.platform.service.impl.recovery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.platform.config.RecoveryProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Optional Slack escalation for high-impact recovery incidents.
 * Disabled by default (app.recovery.alerts.enabled=false).
 */
@Service
public class RecoverySlackAlertService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecoverySlackAlertService.class);

    private final RecoveryProperties recoveryProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public RecoverySlackAlertService(RecoveryProperties recoveryProperties, ObjectMapper objectMapper) {
        this.recoveryProperties = recoveryProperties;
        this.objectMapper = objectMapper;
    }

    public void notifyHighImpact(String title, Map<String, Object> context) {
        RecoveryProperties.Alerts alerts = recoveryProperties.getAlerts();
        if (alerts == null || !alerts.isEnabled()) {
            return;
        }
        String webhookUrl = alerts.getSlackWebhookUrl();
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }

        String text = buildText(title, context);
        Map<String, Object> payload = Map.of("text", text);

        try {
            String body = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl.trim()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .exceptionally(exception -> {
                        LOGGER.warn("Recovery Slack alert failed: {}", exception.getMessage());
                        return null;
                    });
        } catch (IllegalArgumentException | JsonProcessingException exception) {
            LOGGER.warn("Recovery Slack alert payload build failed: {}", exception.getMessage());
        }
    }

    private static String buildText(String title, Map<String, Object> context) {
        StringBuilder builder = new StringBuilder();
        builder.append("*Recovery Alert* ").append(Instant.now()).append("\n");
        builder.append(title == null ? "Incident" : title).append("\n");

        if (context != null && !context.isEmpty()) {
            LinkedHashMap<String, Object> ordered = new LinkedHashMap<>(context);
            ordered.forEach((key, value) -> builder
                    .append("• ")
                    .append(key)
                    .append(": ")
                    .append(value == null ? "null" : String.valueOf(value))
                    .append("\n"));
        }
        return builder.toString().trim();
    }
}

