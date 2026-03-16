package com.noura.notification.dto;

import com.noura.notification.domain.enums.NotificationCategory;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID recipientUserId,
        NotificationCategory category,
        String title,
        String body,
        boolean read,
        Instant createdAt
) {
}
