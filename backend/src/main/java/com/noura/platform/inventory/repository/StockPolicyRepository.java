package com.noura.platform.inventory.repository;

import com.noura.platform.inventory.domain.StockPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockPolicyRepository extends JpaRepository<StockPolicy, String> {

    Optional<StockPolicy> findByProduct_IdAndWarehouse_Id(String productId, String warehouseId);

    Optional<StockPolicy> findByProduct_IdAndWarehouseIsNull(String productId);
}
