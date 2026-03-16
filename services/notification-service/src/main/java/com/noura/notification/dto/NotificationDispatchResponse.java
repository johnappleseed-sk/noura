package com.noura.notification.dto;

import com.noura.notification.domain.enums.NotificationChannel;
import com.noura.notification.domain.enums.NotificationStatus;
import com.noura.notification.domain.enums.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationDispatchResponse(
        UUID id,
        UUID recipientUserId,
        NotificationType type,
        NotificationChannel channel,
        NotificationStatus status,
        Instant createdAt,
        Instant sentAt,
        Instant failedAt,
        String failureReason
) {
}

