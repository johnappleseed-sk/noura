package com.noura.platform.listener;

import com.noura.platform.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true")
public class OrderKafkaListener {

    /**
     * Handles order created.
     *
     * @param event The domain event payload consumed by this handler.
     */
    @KafkaListener(topics = "${app.kafka.topic-order-created}", groupId = "${spring.application.name}-order-events")
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Kafka order event received: orderId={}, userId={}, storeId={}", event.orderId(), event.userId(), event.storeId());
    }
}
