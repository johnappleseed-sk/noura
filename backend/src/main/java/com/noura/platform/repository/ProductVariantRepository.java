package com.noura.platform.repository;

import com.noura.platform.domain.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {
    /**
     * Finds by product id.
     *
     * @param productId The product id used to locate the target record.
     * @return A list of matching items.
     */
    List<ProductVariant> findByProductId(UUID productId);

    /**
     * Deletes by product id.
     *
     * @param productId The product id used to locate the target record.
     */
    void deleteByProductId(UUID productId);

    /**
     * Finds by sku ignore case.
     *
     * @param sku The sku value.
     * @return The result of find by sku ignore case.
     */
    Optional<ProductVariant> findBySkuIgnoreCase(String sku);
}
