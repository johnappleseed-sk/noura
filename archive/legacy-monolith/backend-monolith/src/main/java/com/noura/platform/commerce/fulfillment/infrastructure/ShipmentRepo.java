package com.noura.platform.commerce.fulfillment.infrastructure;

import com.noura.platform.commerce.fulfillment.domain.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShipmentRepo extends JpaRepository<Shipment, Long> {
    Optional<Shipment> findFirstByOrder_IdOrderByCreatedAtDesc(Long orderId);

    Optional<Shipment> findTopByOrder_IdOrderByCreatedAtDesc(Long orderId);
}
