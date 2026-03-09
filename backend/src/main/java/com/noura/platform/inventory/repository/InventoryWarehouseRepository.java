package com.noura.platform.inventory.repository;

import com.noura.platform.inventory.domain.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface InventoryWarehouseRepository extends JpaRepository<Warehouse, String>, JpaSpecificationExecutor<Warehouse> {

    Optional<Warehouse> findByIdAndDeletedAtIsNull(String id);

    boolean existsByWarehouseCodeIgnoreCaseAndDeletedAtIsNull(String warehouseCode);

    boolean existsByWarehouseCodeIgnoreCaseAndDeletedAtIsNullAndIdNot(String warehouseCode, String id);
}
