package com.noura.promotion.repository;

import com.noura.promotion.domain.entity.PromotionApplicationRecord;
import com.noura.promotion.domain.enums.PromotionApplicableEntityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Repository for promotion scope application mappings.
 */
public interface PromotionApplicationRecordRepository extends JpaRepository<PromotionApplicationRecord, UUID> {

    /**
     * Finds mappings for one promotion.
     *
     * @param promotionId promotion identifier
     * @return scope mappings
     */
    List<PromotionApplicationRecord> findByPromotionId(UUID promotionId);

    /**
     * Deletes mappings for one promotion.
     *
     * @param promotionId promotion identifier
     */
    void deleteByPromotionId(UUID promotionId);

    /**
     * Finds mappings by applicable entity scope.
     *
     * @param applicableEntityType scope type
     * @param applicableEntityId scope identifier
     * @return matching scope mappings
     */
    List<PromotionApplicationRecord> findByApplicableEntityTypeAndApplicableEntityId(
            PromotionApplicableEntityType applicableEntityType,
            UUID applicableEntityId
    );

    /**
     * Finds scope mappings for a batch of promotions.
     *
     * @param promotionIds promotion identifiers
     * @return matching scope mappings
     */
    List<PromotionApplicationRecord> findByPromotionIdIn(Collection<UUID> promotionIds);
}
