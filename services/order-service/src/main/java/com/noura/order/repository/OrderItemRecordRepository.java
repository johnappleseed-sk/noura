package com.noura.order.repository;

import com.noura.order.domain.entity.OrderItemRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for order line item snapshots.
 */
public interface OrderItemRecordRepository extends JpaRepository<OrderItemRecord, UUID> {

    /**
     * Lists all items belonging to one order.
     *
     * @param orderId order identifier
     * @return ordered order items
     */
    List<OrderItemRecord> findByOrderIdOrderByLineNumberAsc(UUID orderId);
}

