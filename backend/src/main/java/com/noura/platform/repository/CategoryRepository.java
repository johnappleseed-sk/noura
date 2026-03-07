package com.noura.platform.repository;

import com.noura.platform.domain.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    /**
     * Finds by name ignore case.
     *
     * @param name The name value.
     * @return The result of find by name ignore case.
     */
    Optional<Category> findByNameIgnoreCase(String name);

    /**
     * Finds by parent is null.
     *
     * @return A list of matching items.
     */
    List<Category> findByParentIsNullOrderByNameAsc();

    /**
     * Checks if any category exists with the given parent id.
     *
     * @param parentId The parent id value.
     * @return The result of exists by parent id.
     */
    boolean existsByParentId(UUID parentId);
}
