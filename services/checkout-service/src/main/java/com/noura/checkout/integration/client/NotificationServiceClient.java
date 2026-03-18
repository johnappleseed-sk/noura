package com.noura.checkout.integration.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

/**
 * REST adapter to notification-service internal command API.
 *
 * <p>This client is best-effort and never blocks checkout success paths.</p>
 */
@Slf4j
@Component
public class NotificationServiceClient {

    private static final String HEADER_CORRELATION = "X-Correlation-ID";
    private static final String HEADER_INTERNAL_API_KEY = "X-Internal-Api-Key";

    private final RestClient restClient;
    private final String internalApiKey;

    /**
     * Creates notification-service REST adapter.
     *
     * @param builder rest client builder
     * @param baseUrl notification-service base URL
     * @param internalApiKey optional internal API key
     */
    public NotificationServiceClient(
            RestClient.Builder builder,
            @Value("${services.notification.base-url:http://localhost:8083}") String baseUrl,
            @Value("${services.notification.internal-api-key:}") String internalApiKey
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.internalApiKey = internalApiKey;
    }

    /**
     * Sends best-effort order placed notification to one customer.
     *
     * @param targetUserId internal customer profile identifier
     * @param orderNumber order number
     * @param correlationId correlation ID for tracing
     */
    public void sendOrderPlacedNotification(UUID targetUserId, String orderNumber, String correlationId) {
        if (targetUserId == null) {
            log.debug("Skipping internal notification because target user ID is missing");
            return;
        }

        try {
            restClient.post()
                    .uri("/internal/notifications")
                    .headers(headers -> {
                        if (correlationId != null && !correlationId.isBlank()) {
                            headers.set(HEADER_CORRELATION, correlationId);
                        }
                        if (internalApiKey != null && !internalApiKey.isBlank()) {
                            headers.set(HEADER_INTERNAL_API_KEY, internalApiKey.trim());
                        }
                    })
                    .body(new InternalNotificationCommand(
                            targetUserId,
                            "ORDER",
                            "Order placed",
                            "Your order " + orderNumber + " has been placed successfully."
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            log.warn("Order notification dispatch failed: status={} body={}",
                    ex.getStatusCode().value(), ex.getResponseBodyAsString());
        } catch (ResourceAccessException ex) {
            log.warn("Order notification dispatch skipped because notification-service is unreachable: {}", ex.getMessage());
        }
    }

    /**
     * Internal notification command payload.
     *
     * @param targetUserId target user identifier
     * @param category notification category
     * @param title message title
     * @param body message body
     */
    private record InternalNotificationCommand(
            UUID targetUserId,
            String category,
            String title,
            String body
    ) {
    }
}
