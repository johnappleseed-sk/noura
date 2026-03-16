package com.noura.catalog.repository;

import com.noura.catalog.domain.entity.CatalogProductInventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CatalogProductInventoryRepository extends JpaRepository<CatalogProductInventory, UUID> {
    List<CatalogProductInventory> findByProductIdIn(Collection<UUID> productIds);

    List<CatalogProductInventory> findByProductId(UUID productId);
}
