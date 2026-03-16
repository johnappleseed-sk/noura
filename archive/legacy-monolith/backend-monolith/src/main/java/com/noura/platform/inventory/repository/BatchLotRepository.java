package com.noura.platform.inventory.repository;

import com.noura.platform.inventory.domain.BatchLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface BatchLotRepository extends JpaRepository<BatchLot, String>, JpaSpecificationExecutor<BatchLot> {

    Optional<BatchLot> findByIdAndDeletedAtIsNull(String batchId);

    Optional<BatchLot> findByProduct_IdAndLotNumberIgnoreCaseAndDeletedAtIsNull(String productId, String lotNumber);
}
