package com.noura.platform.repository;

import com.noura.platform.domain.entity.ProductInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductInventoryRepository extends JpaRepository<ProductInventory, UUID> {
    /**
     * Finds by product id and store id.
     *
     * @param productId The product id used to locate the target record.
     * @param storeId The store id used to locate the target record.
     * @return The result of find by product id and store id.
     */
    Optional<ProductInventory> findByProductIdAndStoreId(UUID productId, UUID storeId);

    /**
     * Finds by product id.
     *
     * @param productId The product id used to locate the target record.
     * @return A list of matching items.
     */
    List<ProductInventory> findByProductId(UUID productId);

    /**
     * Deletes by product id.
     *
     * @param productId The product id used to locate the target record.
     */
    void deleteByProductId(UUID productId);

    /**
     * Decrements stock when inventory exists and enough units are available.
     *
     * @param productId The product id used to locate the target record.
     * @param storeId The store id used to locate the target record.
     * @param quantity The quantity value.
     * @return The number of updated rows.
     */
    @Modifying
    @Query("""
            update ProductInventory i
            set i.stock = i.stock - :quantity
            where i.product.id = :productId
              and i.store.id = :storeId
              and i.stock >= :quantity
            """)
    int decrementStockIfAvailable(
            @Param("productId") UUID productId,
            @Param("storeId") UUID storeId,
            @Param("quantity") int quantity
    );
}
