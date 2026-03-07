package com.noura.platform.dto.notification;

import com.noura.platform.domain.enums.NotificationCategory;

import java.time.Instant;
import java.util.UUID;

public record NotificationDto(
        UUID id,
        UUID targetUserId,
        NotificationCategory category,
        String title,
        String body,
        boolean read,
        Instant createdAt
) {
}
