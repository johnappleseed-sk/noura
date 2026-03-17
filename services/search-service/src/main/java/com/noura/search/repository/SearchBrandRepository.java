package com.noura.search.repository;

import com.noura.search.domain.entity.SearchBrand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Read-only repository for canonical brand metadata used during search projection rebuilds.
 */
public interface SearchBrandRepository extends JpaRepository<SearchBrand, UUID> {
    /**
     * Loads a brand map for rebuild operations.
     *
     * @param ids brand identifiers
     * @return brand list
     */
    List<SearchBrand> findByIdIn(Collection<UUID> ids);
}
