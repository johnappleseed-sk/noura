package com.noura.catalog.repository;

import com.noura.catalog.domain.entity.CatalogBrand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CatalogBrandRepository extends JpaRepository<CatalogBrand, UUID> {
    List<CatalogBrand> findByNameContainingIgnoreCase(String name);

    List<CatalogBrand> findTop10ByNameContainingIgnoreCaseOrderByNameAsc(String name);

    List<CatalogBrand> findByIdIn(Collection<UUID> ids);
}
