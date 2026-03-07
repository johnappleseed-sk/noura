package com.noura.platform.repository;

import com.noura.platform.domain.entity.PromotionApplication;
import com.noura.platform.domain.enums.PromotionApplicableEntityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PromotionApplicationRepository extends JpaRepository<PromotionApplication, UUID> {
    /**
     * Finds by applicable entity type and applicable entity id.
     *
     * @param type The type value.
     * @param entityId The entity id used to locate the target record.
     * @return A list of matching items.
     */
    List<PromotionApplication> findByApplicableEntityTypeAndApplicableEntityId(
            PromotionApplicableEntityType type,
            UUID entityId
    );

    /**
     * Finds by promotion id.
     *
     * @param promotionId The promotion id used to locate the target record.
     * @return A list of matching items.
     */
    List<PromotionApplication> findByPromotionId(UUID promotionId);
}
