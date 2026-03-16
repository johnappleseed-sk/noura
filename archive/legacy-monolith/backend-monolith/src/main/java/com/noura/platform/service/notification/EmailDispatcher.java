package com.noura.platform.service.notification;

import com.noura.platform.domain.entity.NotificationMessage;
import com.noura.platform.domain.enums.NotificationChannel;
import com.noura.platform.domain.enums.NotificationStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class EmailDispatcher implements NotificationDispatcher {

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.EMAIL;
    }

    @Override
    public DispatchResult dispatch(NotificationMessage message) {
        return new DispatchResult(NotificationStatus.SENT, Instant.now(), null, null);
    }
}
