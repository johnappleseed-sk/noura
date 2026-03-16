package com.noura.platform.commerce.orders.infrastructure;

import com.noura.platform.commerce.orders.domain.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepo extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrder_Id(Long orderId);

    List<OrderItem> findByOrder_IdOrderByIdAsc(Long orderId);

    void deleteByOrder_Id(Long orderId);
}
