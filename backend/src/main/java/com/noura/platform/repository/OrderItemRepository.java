package com.noura.platform.repository;

import com.noura.platform.domain.entity.OrderItem;
import com.noura.platform.domain.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
    /**
     * Finds by order id.
     *
     * @param orderId The order id used to locate the target record.
     * @return A list of matching items.
     */
    List<OrderItem> findByOrderId(UUID orderId);

    /**
     * Finds by order in.
     *
     * @param orders The orders value.
     * @return A list of matching items.
     */
    List<OrderItem> findByOrderIn(List<Order> orders);
}
