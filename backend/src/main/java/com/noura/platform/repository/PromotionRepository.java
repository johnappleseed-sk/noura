package com.noura.platform.repository;

import com.noura.platform.domain.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromotionRepository extends JpaRepository<Promotion, UUID>, JpaSpecificationExecutor<Promotion> {
    /**
     * Finds by active true.
     *
     * @return A list of matching items.
     */
    List<Promotion> findByActiveTrue();

    List<Promotion> findByActiveTrueAndArchivedFalse();

    Optional<Promotion> findByCodeIgnoreCase(String code);
}
