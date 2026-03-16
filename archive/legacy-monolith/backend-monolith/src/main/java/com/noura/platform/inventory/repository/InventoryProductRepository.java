package com.noura.platform.inventory.repository;

import com.noura.platform.inventory.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface InventoryProductRepository extends JpaRepository<Product, String>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByIdAndDeletedAtIsNull(String id);

    boolean existsBySkuIgnoreCaseAndDeletedAtIsNull(String sku);

    boolean existsBySkuIgnoreCaseAndDeletedAtIsNullAndIdNot(String sku, String id);
}
