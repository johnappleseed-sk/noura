package com.noura.pricing.repository;

import com.noura.pricing.domain.entity.LegacyPriceList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for admin-facing legacy price-list metadata.
 */
public interface LegacyPriceListRepository extends JpaRepository<LegacyPriceList, UUID> {

    List<LegacyPriceList> findAllByOrderByNameAsc();
}
