package com.noura.platform.commerce.multistore.infrastructure;

import com.noura.platform.commerce.multistore.domain.StoreInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface StoreInventoryRepo extends JpaRepository<StoreInventory, Long> {

    Optional<StoreInventory> findByStoreIdAndProductId(Long storeId, Long productId);

    List<StoreInventory> findByStoreId(Long storeId);

    List<StoreInventory> findByProductId(Long productId);

    @Query("SELECT SUM(si.quantityOnHand) FROM StoreInventory si WHERE si.productId = :productId")
    BigDecimal sumQuantityByProductId(Long productId);

    @Query("SELECT si FROM StoreInventory si WHERE si.store.id = :storeId AND si.quantityOnHand <= si.reorderPoint")
    List<StoreInventory> findLowStockByStoreId(Long storeId);
}
