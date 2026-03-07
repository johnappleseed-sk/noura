package com.noura.platform.repository;

import com.noura.platform.domain.entity.AttributeSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AttributeSetRepository extends JpaRepository<AttributeSet, UUID> {
    /**
     * Finds by name ignore case.
     *
     * @param name The name value.
     * @return The result of find by name ignore case.
     */
    Optional<AttributeSet> findByNameIgnoreCase(String name);
}
