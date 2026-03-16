package com.noura.platform.service.notification;

import com.noura.platform.dto.notification.SendNotificationRequest;
import com.noura.platform.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Default in-process adapter used until remote notification extraction is enabled.
 */
@Service
@Primary
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.notifications.remote",
        name = "enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class LocalNotificationCommandAdapter implements NotificationCommandPort {

    private final NotificationService notificationService;

    @Override
    public void pushToUser(UUID targetUserId, SendNotificationRequest request) {
        notificationService.pushToUser(targetUserId, request);
    }
}

