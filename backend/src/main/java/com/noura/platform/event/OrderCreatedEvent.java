package com.noura.platform.event;

import java.util.UUID;

public record OrderCreatedEvent(UUID orderId, UUID userId, UUID storeId) {
}
