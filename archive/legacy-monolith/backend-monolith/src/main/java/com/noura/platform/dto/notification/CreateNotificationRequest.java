package com.noura.platform.dto.notification;

import com.noura.platform.domain.enums.NotificationChannel;
import com.noura.platform.domain.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateNotificationRequest(
        @NotNull UUID recipientUserId,
        @NotNull NotificationType type,
        @NotNull NotificationChannel channel,
        @NotBlank @Size(max = 255) String subject,
        @NotBlank @Size(max = 4000) String body,
        @Size(max = 80) String relatedEntityType,
        UUID relatedEntityId
) {
}
