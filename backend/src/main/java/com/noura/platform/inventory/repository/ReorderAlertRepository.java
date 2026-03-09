package com.noura.platform.inventory.repository;

import com.noura.platform.inventory.domain.ReorderAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReorderAlertRepository extends JpaRepository<ReorderAlert, String> {

    Optional<ReorderAlert> findFirstByProduct_IdAndWarehouse_IdAndResolvedAtIsNullOrderByCreatedAtDesc(String productId, String warehouseId);
}
