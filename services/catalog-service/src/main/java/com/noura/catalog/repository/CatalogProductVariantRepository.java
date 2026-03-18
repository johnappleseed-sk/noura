package com.noura.catalog.repository;

import com.noura.catalog.domain.entity.CatalogProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CatalogProductVariantRepository extends JpaRepository<CatalogProductVariant, UUID> {
    List<CatalogProductVariant> findByProductIdInAndActiveTrueOrderByProductIdAscSkuAsc(Collection<UUID> productIds);

    Optional<CatalogProductVariant> findByIdAndActiveTrue(UUID id);
}
