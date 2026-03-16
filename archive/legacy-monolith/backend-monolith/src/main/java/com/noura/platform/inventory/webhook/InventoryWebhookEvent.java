package com.noura.platform.inventory.webhook;

public record InventoryWebhookEvent(
        String eventCode,
        Object payload
) {
}
