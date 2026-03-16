package com.noura.platform.repository;

import com.noura.platform.domain.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, UUID> {
    /**
     * Finds by variant id order by created at desc.
     *
     * @param variantId The variant id used to locate the target record.
     * @return A list of matching items.
     */
    List<InventoryTransaction> findByVariantIdOrderByCreatedAtDesc(UUID variantId);
}
