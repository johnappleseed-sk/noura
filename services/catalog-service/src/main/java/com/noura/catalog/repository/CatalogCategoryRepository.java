package com.noura.catalog.repository;

import com.noura.catalog.domain.entity.CatalogCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CatalogCategoryRepository extends JpaRepository<CatalogCategory, UUID> {
    List<CatalogCategory> findAllByActiveTrueOrderByLevelAscNameAsc();

    List<CatalogCategory> findByNameContainingIgnoreCase(String name);

    List<CatalogCategory> findByIdIn(Collection<UUID> ids);
}
