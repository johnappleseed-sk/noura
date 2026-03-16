package com.noura.pricing.repository;

import com.noura.pricing.domain.entity.PricingCurrency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Persistence gateway for supported pricing currencies.
 */
public interface PricingCurrencyRepository extends JpaRepository<PricingCurrency, String> {

    /**
     * Retrieves active default currency row.
     *
     * @return default currency row when configured
     */
    Optional<PricingCurrency> findByDefaultCurrencyTrueAndActiveTrue();
}

