package com.noura.platform.repository;

import com.noura.platform.domain.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {
    /**
     * Finds top10 by trending true order by popularity score desc.
     *
     * @return A list of matching items.
     */
    List<Product> findTop10ByTrendingTrueOrderByPopularityScoreDesc();

    /**
     * Finds top10 by best seller true order by popularity score desc.
     *
     * @return A list of matching items.
     */
    List<Product> findTop10ByBestSellerTrueOrderByPopularityScoreDesc();

    /**
     * Checks if any product exists with the given category id.
     *
     * @param categoryId The category id value.
     * @return The result of exists by category id.
     */
    boolean existsByCategoryId(UUID categoryId);
}
