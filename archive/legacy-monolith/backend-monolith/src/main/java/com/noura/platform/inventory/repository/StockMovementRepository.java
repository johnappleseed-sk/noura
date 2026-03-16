package com.noura.platform.inventory.repository;

import com.noura.platform.inventory.domain.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface StockMovementRepository extends JpaRepository<StockMovement, String>, JpaSpecificationExecutor<StockMovement> {

    Optional<StockMovement> findByIdAndDeletedAtIsNull(String movementId);
}
