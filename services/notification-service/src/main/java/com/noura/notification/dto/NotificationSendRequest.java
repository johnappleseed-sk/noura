package com.noura.notification.dto;

import com.noura.notification.domain.enums.NotificationCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NotificationSendRequest(
        @NotNull NotificationCategory category,
        @NotBlank String title,
        @NotBlank String body
) {
}
