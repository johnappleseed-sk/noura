package com.noura.platform.service.impl;

import com.noura.platform.dto.notification.NotificationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisNotificationSubscriber {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handles message.
     *
     * @param payload The message payload for this operation.
     * @param channel The channel value.
     */
    public void onMessage(Object payload, String channel) {
        if (payload instanceof NotificationDto dto) {
            if (dto.targetUserId() == null) {
                messagingTemplate.convertAndSend("/topic/notifications", dto);
            } else {
                messagingTemplate.convertAndSend("/topic/notifications/" + dto.targetUserId(), dto);
            }
        }
    }
}
