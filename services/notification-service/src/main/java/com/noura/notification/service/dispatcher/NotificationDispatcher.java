package com.noura.notification.service.dispatcher;

import com.noura.notification.domain.NotificationMessage;
import com.noura.notification.domain.enums.NotificationChannel;
import com.noura.notification.domain.enums.NotificationStatus;

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

