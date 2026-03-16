package com.noura.platform.inventory.service;

import com.noura.platform.inventory.dto.webhook.WebhookSubscriptionRequest;
import com.noura.platform.inventory.dto.webhook.WebhookSubscriptionResponse;

import java.util.List;

public interface WebhookSubscriptionService {

    List<WebhookSubscriptionResponse> listSubscriptions();

    WebhookSubscriptionResponse getSubscription(String subscriptionId);

    WebhookSubscriptionResponse createSubscription(WebhookSubscriptionRequest request);

    WebhookSubscriptionResponse updateSubscription(String subscriptionId, WebhookSubscriptionRequest request);

    void deleteSubscription(String subscriptionId);
}
