package com.noura.platform.service.notification;

import com.noura.platform.domain.entity.NotificationMessage;
import com.noura.platform.domain.enums.NotificationChannel;
import com.noura.platform.domain.enums.NotificationStatus;

import java.time.Instant;

public interface NotificationDispatcher {
    boolean supports(NotificationChannel channel);

    DispatchResult dispatch(NotificationMessage message);

    record DispatchResult(
            NotificationStatus status,
            Instant sentAt,
            Instant failedAt,
            String failureReason
    ) {
    }
}
