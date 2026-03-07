package com.noura.platform.service;

import com.noura.platform.dto.notification.NotificationDto;
import com.noura.platform.dto.notification.SendNotificationRequest;

import java.util.List;
import java.util.UUID;

public interface NotificationService {
    /**
     * Retrieves my notifications.
     *
     * @return A list of matching items.
     */
    List<NotificationDto> myNotifications();

    /**
     * Executes unread count.
     *
     * @return The result of unread count.
     */
    long unreadCount();

    /**
     * Marks as read.
     *
     * @param notificationId The notification id used to locate the target record.
     * @return The mapped DTO representation.
     */
    NotificationDto markAsRead(UUID notificationId);

    /**
     * Marks all as read.
     *
     * @return The number of updated records.
     */
    int markAllAsRead();

    /**
     * Pushes to user.
     *
     * @param targetUserId The target user id used to locate the target record.
     * @param request The request payload for this operation.
     * @return The mapped DTO representation.
     */
    NotificationDto pushToUser(UUID targetUserId, SendNotificationRequest request);

    /**
     * Broadcasts message.
     *
     * @param request The request payload for this operation.
     */
    void broadcast(SendNotificationRequest request);
}
