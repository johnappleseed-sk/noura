package com.noura.shipping.repository;

import com.noura.shipping.domain.entity.ShipmentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for shipment aggregate persistence and idempotent lookup patterns.
 */
public interface ShipmentRecordRepository extends JpaRepository<ShipmentRecord, UUID> {

    /**
     * Finds an idempotent shipment create result.
     *
     * @param orderId order identifier
     * @param customerRef customer reference
     * @param idempotencyKey idempotency key
     * @return matching shipment, when present
     */
    Optional<ShipmentRecord> findByOrderIdAndCustomerRefAndIdempotencyKey(UUID orderId, String customerRef, String idempotencyKey);

    /**
     * Finds shipment records for one order, newest first.
     *
     * @param orderId order identifier
     * @return shipment list
     */
    List<ShipmentRecord> findByOrderIdOrderByUpdatedAtDesc(UUID orderId);

    /**
     * Finds one shipment by reference.
     *
     * @param shipmentReference shipment reference
     * @return matching shipment
     */
    Optional<ShipmentRecord> findByShipmentReference(String shipmentReference);

    /**
     * Loads one shipment with a write lock for lifecycle changes.
     *
     * @param id shipment identifier
     * @return locked shipment
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select shipment from ShipmentRecord shipment where shipment.id = :id")
    Optional<ShipmentRecord> findByIdForUpdate(UUID id);
}
