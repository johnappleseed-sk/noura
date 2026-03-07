package com.noura.platform.repository;

import com.noura.platform.domain.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BrandRepository extends JpaRepository<Brand, UUID> {
    /**
     * Finds by name ignore case.
     *
     * @param name The name value.
     * @return The result of find by name ignore case.
     */
    Optional<Brand> findByNameIgnoreCase(String name);
}
