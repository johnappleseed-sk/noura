package com.noura.order.repository;

import com.noura.order.domain.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for order status history records.
 */
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, UUID> {

    /**
     * Lists status history events for one order in chronological order.
     *
     * @param orderId order identifier
     * @return ordered timeline events
     */
    List<OrderStatusHistory> findByOrderIdOrderByChangedAtAsc(UUID orderId);
}

