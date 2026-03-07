package com.noura.platform.commerce.orders.infrastructure;

import com.noura.platform.commerce.orders.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface OrderRepo extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByCustomerAccount_IdOrderByPlacedAtDesc(Long customerAccountId);
}
