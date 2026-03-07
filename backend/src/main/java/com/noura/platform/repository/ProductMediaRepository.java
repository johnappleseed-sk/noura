package com.noura.platform.repository;

import com.noura.platform.domain.entity.ProductMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductMediaRepository extends JpaRepository<ProductMedia, UUID> {
    /**
     * Finds by product id order by sort order asc.
     *
     * @param productId The product id used to locate the target record.
     * @return A list of matching items.
     */
    List<ProductMedia> findByProductIdOrderBySortOrderAsc(UUID productId);

    /**
     * Deletes by product id.
     *
     * @param productId The product id used to locate the target record.
     */
    void deleteByProductId(UUID productId);
}
