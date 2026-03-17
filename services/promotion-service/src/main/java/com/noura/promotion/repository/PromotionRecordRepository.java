package com.noura.promotion.repository;

import com.noura.promotion.domain.entity.PromotionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for promotion aggregate persistence and lookups.
 */
public interface PromotionRecordRepository extends JpaRepository<PromotionRecord, UUID> {

    /**
     * Finds non-archived active promotions.
     *
     * @return active non-archived promotions
     */
    List<PromotionRecord> findByActiveTrueAndArchivedFalse();

    /**
     * Finds by case-insensitive business code.
     *
     * @param code business code
     * @return matching promotion
     */
    Optional<PromotionRecord> findByCodeIgnoreCase(String code);

    /**
     * Finds by case-insensitive coupon code.
     *
     * @param couponCode coupon code
     * @return matching promotion
     */
    Optional<PromotionRecord> findByCouponCodeIgnoreCase(String couponCode);
}
