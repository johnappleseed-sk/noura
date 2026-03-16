package com.noura.platform.listener;

import com.noura.platform.domain.enums.NotificationCategory;
import com.noura.platform.dto.notification.SendNotificationRequest;
import com.noura.platform.event.OrderCreatedEvent;
import com.noura.platform.service.notification.NotificationCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final NotificationCommandPort notificationCommandPort;

    /**
     * Handles order created.
     *
     * @param event The domain event payload consumed by this handler.
     */
    @Async
    @TransactionalEventListener
    public void onOrderCreated(OrderCreatedEvent event) {
        notificationCommandPort.pushToUser(
                event.userId(),
                new SendNotificationRequest(
                        event.userId(),
                        NotificationCategory.ORDER,
                        "Order received",
                        "Your order " + event.orderId() + " has been created successfully."
                )
        );
    }
}
