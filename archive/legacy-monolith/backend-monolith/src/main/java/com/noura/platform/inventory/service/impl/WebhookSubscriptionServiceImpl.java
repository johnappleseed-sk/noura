package com.noura.platform.inventory.service.impl;

import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.inventory.domain.WebhookSubscription;
import com.noura.platform.inventory.dto.webhook.WebhookSubscriptionRequest;
import com.noura.platform.inventory.dto.webhook.WebhookSubscriptionResponse;
import com.noura.platform.inventory.repository.WebhookSubscriptionRepository;
import com.noura.platform.inventory.service.WebhookSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WebhookSubscriptionServiceImpl implements WebhookSubscriptionService {

    private final WebhookSubscriptionRepository webhookSubscriptionRepository;

    @Override
    @Transactional(transactionManager = "inventoryTransactionManager", readOnly = true)
    public List<WebhookSubscriptionResponse> listSubscriptions() {
        return webhookSubscriptionRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(transactionManager = "inventoryTransactionManager", readOnly = true)
    public WebhookSubscriptionResponse getSubscription(String subscriptionId) {
        return toResponse(getEntity(subscriptionId));
    }

    @Override
    @Transactional(transactionManager = "inventoryTransactionManager")
    public WebhookSubscriptionResponse createSubscription(WebhookSubscriptionRequest request) {
        WebhookSubscription subscription = new WebhookSubscription();
        apply(subscription, request);
        return toResponse(webhookSubscriptionRepository.save(subscription));
    }

    @Override
    @Transactional(transactionManager = "inventoryTransactionManager")
    public WebhookSubscriptionResponse updateSubscription(String subscriptionId, WebhookSubscriptionRequest request) {
        WebhookSubscription subscription = getEntity(subscriptionId);
        apply(subscription, request);
        return toResponse(webhookSubscriptionRepository.save(subscription));
    }

    @Override
    @Transactional(transactionManager = "inventoryTransactionManager")
    public void deleteSubscription(String subscriptionId) {
        webhookSubscriptionRepository.delete(getEntity(subscriptionId));
    }

    private WebhookSubscription getEntity(String subscriptionId) {
        return webhookSubscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new NotFoundException("WEBHOOK_NOT_FOUND", "Webhook subscription not found"));
    }

    private void apply(WebhookSubscription subscription, WebhookSubscriptionRequest request) {
        subscription.setEventCode(request.eventCode().trim().toLowerCase());
        subscription.setEndpointUrl(request.endpointUrl().trim());
        if (request.secretToken() != null) {
            subscription.setSecretToken(StringUtils.hasText(request.secretToken()) ? request.secretToken().trim() : null);
        }
        if (request.active() != null) {
            subscription.setActive(request.active());
        }
        if (request.timeoutMs() != null) {
            subscription.setTimeoutMs(request.timeoutMs());
        }
        if (request.retryCount() != null) {
            subscription.setRetryCount(request.retryCount());
        }
    }

    private WebhookSubscriptionResponse toResponse(WebhookSubscription subscription) {
        return new WebhookSubscriptionResponse(
                subscription.getId(),
                subscription.getEventCode(),
                subscription.getEndpointUrl(),
                subscription.isActive(),
                subscription.getTimeoutMs(),
                subscription.getRetryCount(),
                subscription.getCreatedAt(),
                subscription.getUpdatedAt()
        );
    }
}
