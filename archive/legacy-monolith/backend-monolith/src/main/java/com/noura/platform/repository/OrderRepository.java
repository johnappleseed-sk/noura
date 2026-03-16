package com.noura.platform.repository;

import com.noura.platform.domain.entity.Order;
import com.noura.platform.domain.entity.UserAccount;
import com.noura.platform.domain.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {
    /**
     * Finds top10 by user order by created at desc.
     *
     * @param user The user context for this operation.
     * @return A list of matching items.
     */
    List<Order> findTop10ByUserOrderByCreatedAtDesc(UserAccount user);

    /**
     * Finds by user and idempotency key.
     *
     * @param user The user context for this operation.
     * @param idempotencyKey The idempotency key value.
     * @return The result of find by user and idempotency key.
     */
    Optional<Order> findByUserAndIdempotencyKey(UserAccount user, String idempotencyKey);

    /**
     * Finds by created at between and status in.
     *
     * @param from The from value.
     * @param to The to value.
     * @param statuses The statuses value.
     * @return A list of matching items.
     */
    List<Order> findByCreatedAtBetweenAndStatusIn(Instant from, Instant to, Collection<OrderStatus> statuses);
}
