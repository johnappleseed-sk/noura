package com.noura.checkout.repository;

import com.noura.checkout.domain.entity.CheckoutRequestRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for checkout idempotency records.
 */
public interface CheckoutRequestRecordRepository extends JpaRepository<CheckoutRequestRecord, UUID> {

    /**
     * Finds one idempotency record by customer and idempotency key.
     *
     * @param customerRef customer reference key
     * @param idempotencyKey idempotency key
     * @return optional matching record
     */
    Optional<CheckoutRequestRecord> findByCustomerRefAndIdempotencyKey(String customerRef, String idempotencyKey);

    /**
     * Finds one idempotency record using a pessimistic write lock.
     *
     * @param customerRef customer reference key
     * @param idempotencyKey idempotency key
     * @return optional matching record locked for update
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r
            from CheckoutRequestRecord r
            where r.customerRef = :customerRef
              and r.idempotencyKey = :idempotencyKey
            """)
    Optional<CheckoutRequestRecord> findByCustomerRefAndIdempotencyKeyForUpdate(
            @Param("customerRef") String customerRef,
            @Param("idempotencyKey") String idempotencyKey
    );
}

