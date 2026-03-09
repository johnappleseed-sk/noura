package com.noura.platform.inventory.webhook;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryWebhookPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void publish(String eventCode, Object payload) {
        applicationEventPublisher.publishEvent(new InventoryWebhookEvent(eventCode, payload));
    }
}
