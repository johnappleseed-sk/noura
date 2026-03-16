package com.noura.platform.repository;

import com.noura.platform.domain.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CouponRepository extends JpaRepository<Coupon, UUID> {
    /**
     * Finds by code ignore case and active true.
     *
     * @param code The code value.
     * @return The result of find by code ignore case and active true.
     */
    Optional<Coupon> findByCodeIgnoreCaseAndActiveTrue(String code);
}
