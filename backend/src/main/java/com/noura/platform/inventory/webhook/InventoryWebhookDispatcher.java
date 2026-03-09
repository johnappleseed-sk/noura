package com.noura.platform.inventory.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.platform.inventory.domain.WebhookSubscription;
import com.noura.platform.inventory.repository.WebhookSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryWebhookDispatcher {

    private final WebhookSubscriptionRepository webhookSubscriptionRepository;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void dispatch(InventoryWebhookEvent event) {
        List<WebhookSubscription> subscriptions = webhookSubscriptionRepository
                .findAllByActiveTrueAndEventCodeIgnoreCase(event.eventCode());
        if (subscriptions.isEmpty()) {
            return;
        }
        for (WebhookSubscription subscription : subscriptions) {
            try {
                byte[] payload = objectMapper.writeValueAsBytes(event.payload());
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofMillis(subscription.getTimeoutMs()))
                        .build();
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(subscription.getEndpointUrl()))
                        .timeout(Duration.ofMillis(subscription.getTimeoutMs()))
                        .header("Content-Type", "application/json")
                        .header("X-Noura-Event-Code", event.eventCode())
                        .POST(HttpRequest.BodyPublishers.ofByteArray(payload));
                if (subscription.getSecretToken() != null && !subscription.getSecretToken().isBlank()) {
                    requestBuilder.header("X-Noura-Signature", signature(subscription.getSecretToken(), payload));
                }
                HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 400) {
                    log.warn("Inventory webhook {} returned status {} for {}", event.eventCode(), response.statusCode(), subscription.getEndpointUrl());
                }
            } catch (Exception ex) {
                log.warn("Inventory webhook dispatch failed for {} to {}", event.eventCode(), subscription.getEndpointUrl(), ex);
            }
        }
    }

    private String signature(String secret, byte[] payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(payload));
    }
}
