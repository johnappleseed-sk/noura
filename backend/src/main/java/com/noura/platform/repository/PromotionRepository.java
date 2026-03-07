package com.noura.platform.repository;

import com.noura.platform.domain.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PromotionRepository extends JpaRepository<Promotion, UUID> {
    /**
     * Finds by active true.
     *
     * @return A list of matching items.
     */
    List<Promotion> findByActiveTrue();
}
