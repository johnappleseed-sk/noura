package com.noura.platform.inventory.repository;

import com.noura.platform.inventory.domain.WarehouseBin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface WarehouseBinRepository extends JpaRepository<WarehouseBin, String>, JpaSpecificationExecutor<WarehouseBin> {

    Optional<WarehouseBin> findByIdAndDeletedAtIsNull(String id);

    boolean existsByWarehouse_IdAndBinCodeIgnoreCaseAndDeletedAtIsNull(String warehouseId, String binCode);

    boolean existsByWarehouse_IdAndBinCodeIgnoreCaseAndDeletedAtIsNullAndIdNot(String warehouseId, String binCode, String id);

    boolean existsByWarehouse_IdAndDeletedAtIsNull(String warehouseId);
}
