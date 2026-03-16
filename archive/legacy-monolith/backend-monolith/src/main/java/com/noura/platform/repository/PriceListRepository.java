package com.noura.platform.repository;

import com.noura.platform.domain.entity.PriceList;
import com.noura.platform.domain.enums.PriceListType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PriceListRepository extends JpaRepository<PriceList, UUID> {
    /**
     * Determines whether exists by name ignore case.
     *
     * @param name The name value.
     * @return True when the condition is satisfied; otherwise false.
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Finds by type.
     *
     * @param type The type value.
     * @return A list of matching items.
     */
    List<PriceList> findByType(PriceListType type);
}
