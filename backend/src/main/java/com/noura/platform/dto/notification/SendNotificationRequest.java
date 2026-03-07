package com.noura.platform.dto.notification;

import com.noura.platform.domain.enums.NotificationCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SendNotificationRequest(
        UUID targetUserId,
        @NotNull NotificationCategory category,
        @NotBlank String title,
        @NotBlank String body
) {
}
