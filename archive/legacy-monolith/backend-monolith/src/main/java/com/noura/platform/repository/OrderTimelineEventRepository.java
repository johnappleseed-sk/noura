package com.noura.platform.repository;

import com.noura.platform.domain.entity.OrderTimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderTimelineEventRepository extends JpaRepository<OrderTimelineEvent, UUID> {
    /**
     * Finds by order id order by created at asc.
     *
     * @param orderId The order id used to locate the target record.
     * @return A list of matching items.
     */
    List<OrderTimelineEvent> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
}

