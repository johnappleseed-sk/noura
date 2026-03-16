package com.noura.platform.repository;

import com.noura.platform.domain.entity.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
     * Finds active products by name fragment, ordered by most recently updated.
     *
     * @param name The name fragment.
     * @return Matching products.
     */
    List<Product> findTop20ByActiveTrueAndNameContainingIgnoreCaseOrderByUpdatedAtDesc(String name);

    /**
     * Finds active product by id.
     *
     * @param productId The product id.
     * @return The product when active.
     */
    Optional<Product> findByIdAndActiveTrue(UUID productId);

    /**
     * Checks if barcode already exists.
     *
     * @param barcode The barcode value.
     * @return True when barcode already exists.
     */
    boolean existsByBarcodeIgnoreCase(String barcode);

    boolean existsByProductCodeIgnoreCase(String productCode);

    boolean existsBySlugIgnoreCase(String slug);

    Optional<Product> findByBarcodeIgnoreCase(String barcode);

    Optional<Product> findByDedupeFingerprint(String dedupeFingerprint);

    List<Product> findTop20ByActiveTrueAndNormalizedNameContainingIgnoreCaseOrderByUpdatedAtDesc(String normalizedName);

    List<Product> findTop20ByActiveTrueAndManufacturerPartNumberContainingIgnoreCaseOrderByUpdatedAtDesc(String manufacturerPartNumber);

    @EntityGraph(attributePaths = {"brand", "category"})
    List<Product> findByIdIn(Collection<UUID> ids);

    /**
     * Checks if any product exists with the given category id.
     *
     * @param categoryId The category id value.
     * @return The result of exists by category id.
     */
    boolean existsByCategoryId(UUID categoryId);
}
