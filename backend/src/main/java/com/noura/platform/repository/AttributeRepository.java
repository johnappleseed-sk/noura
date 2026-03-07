package com.noura.platform.repository;

import com.noura.platform.domain.entity.Attribute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AttributeRepository extends JpaRepository<Attribute, UUID> {
    /**
     * Finds by name ignore case.
     *
     * @param name The name value.
     * @return The result of find by name ignore case.
     */
    Optional<Attribute> findByNameIgnoreCase(String name);
}
