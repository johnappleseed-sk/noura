package com.noura.platform.repository;

import com.noura.platform.domain.entity.InventoryReservation;
import com.noura.platform.domain.enums.InventoryReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, UUID> {
    /**
     * Finds by status.
     *
     * @param status The status value.
     * @return A list of matching items.
     */
    List<InventoryReservation> findByStatus(InventoryReservationStatus status);
}
