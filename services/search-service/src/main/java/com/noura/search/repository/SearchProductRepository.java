package com.noura.search.repository;

import com.noura.search.domain.entity.SearchProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Read-only repository for canonical catalog products used to rebuild the search projection.
 */
public interface SearchProductRepository extends JpaRepository<SearchProduct, UUID> {
    /**
     * Returns all products ordered by most-recent source update for rebuild operations.
     *
     * @return source product list
     */
    List<SearchProduct> findAllByOrderByUpdatedAtDesc();
}
