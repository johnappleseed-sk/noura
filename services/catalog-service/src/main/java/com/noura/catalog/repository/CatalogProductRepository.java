package com.noura.catalog.repository;

import com.noura.catalog.domain.entity.CatalogProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CatalogProductRepository extends JpaRepository<CatalogProduct, UUID>, JpaSpecificationExecutor<CatalogProduct> {
    Optional<CatalogProduct> findByIdAndActiveTrue(UUID id);

    List<CatalogProduct> findTop20ByActiveTrueAndNameContainingIgnoreCaseOrderByUpdatedAtDesc(String name);

    List<CatalogProduct> findTop20ByActiveTrueAndTrendingTrueOrderByPopularityScoreDesc();
}
