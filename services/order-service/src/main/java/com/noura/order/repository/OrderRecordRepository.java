package com.noura.order.repository;

import com.noura.order.domain.entity.OrderRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for order aggregate roots.
 */
public interface OrderRecordRepository extends JpaRepository<OrderRecord, UUID>, JpaSpecificationExecutor<OrderRecord> {

    /**
     * Finds one existing order by customer reference and idempotency key.
     *
     * @param customerRef customer reference
     * @param idempotencyKey idempotency key
     * @return matching order when found
     */
    Optional<OrderRecord> findByCustomerRefAndIdempotencyKey(String customerRef, String idempotencyKey);

    /**
     * Lists orders for one customer ordered by placement timestamp descending.
     *
     * @param customerRef customer reference
     * @return customer orders
     */
    List<OrderRecord> findByCustomerRefOrderByPlacedAtDesc(String customerRef);
}

