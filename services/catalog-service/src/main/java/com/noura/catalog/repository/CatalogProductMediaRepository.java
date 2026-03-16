package com.noura.catalog.repository;

import com.noura.catalog.domain.entity.CatalogProductMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CatalogProductMediaRepository extends JpaRepository<CatalogProductMedia, UUID> {
    List<CatalogProductMedia> findByProductIdInOrderByProductIdAscSortOrderAsc(Collection<UUID> productIds);
}
