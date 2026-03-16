package com.noura.platform.dto.notification;

import com.noura.platform.domain.enums.NotificationChannel;
import com.noura.platform.domain.enums.NotificationStatus;
import com.noura.platform.domain.enums.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID recipientUserId,
        NotificationType type,
        NotificationChannel channel,
        String subject,
        String body,
        NotificationStatus status,
        String relatedEntityType,
        UUID relatedEntityId,
        Instant createdAt,
        Instant sentAt,
        Instant failedAt,
        String failureReason
) {
}
