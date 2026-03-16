package com.noura.platform.service;

import com.noura.platform.domain.enums.NotificationStatus;
import com.noura.platform.dto.notification.CreateNotificationRequest;
import com.noura.platform.dto.notification.NotificationResponse;
import com.noura.platform.dto.notification.SendNotificationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationService {
    NotificationResponse createNotification(CreateNotificationRequest request);

    Page<NotificationResponse> listNotifications(NotificationStatus status, UUID recipientUserId, Pageable pageable);

    Page<NotificationResponse> myNotifications(NotificationStatus status, Pageable pageable);

    NotificationResponse pushToUser(UUID targetUserId, SendNotificationRequest request);
}
