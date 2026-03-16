package com.noura.platform.service.notification;

import com.noura.platform.dto.notification.SendNotificationRequest;

import java.util.UUID;

/**
 * Strangler bridge for dispatching notification commands while notification delivery
 * moves from monolith-local execution to a remote notification service.
 */
public interface NotificationCommandPort {
    void pushToUser(UUID targetUserId, SendNotificationRequest request);
}

