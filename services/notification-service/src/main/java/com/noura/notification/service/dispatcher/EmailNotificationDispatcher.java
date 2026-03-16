package com.noura.notification.service.dispatcher;

import com.noura.notification.domain.NotificationMessage;
import com.noura.notification.domain.enums.NotificationChannel;
import com.noura.notification.domain.enums.NotificationStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class EmailNotificationDispatcher implements NotificationDispatcher {

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.EMAIL;
    }

    @Override
    public DispatchResult dispatch(NotificationMessage message) {
        return new DispatchResult(NotificationStatus.SENT, Instant.now(), null, null);
    }
}

