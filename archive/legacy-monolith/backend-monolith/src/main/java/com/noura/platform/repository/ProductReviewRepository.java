package com.noura.platform.repository;

import com.noura.platform.domain.entity.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductReviewRepository extends JpaRepository<ProductReview, UUID> {
    /**
     * Finds by product id order by created at desc.
     *
     * @param productId The product id used to locate the target record.
     * @return A list of matching items.
     */
    List<ProductReview> findByProductIdOrderByCreatedAtDesc(UUID productId);
}
