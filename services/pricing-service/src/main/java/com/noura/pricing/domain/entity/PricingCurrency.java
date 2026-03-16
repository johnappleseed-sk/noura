package com.noura.pricing.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Reference entity representing a supported pricing currency.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pricing_currencies")
public class PricingCurrency {

    @Id
    @Column(name = "code", length = 3, nullable = false, updatable = false)
    private String code;

    @Column(name = "name", length = 80, nullable = false)
    private String name;

    @Column(name = "decimal_places", nullable = false)
    private short decimalPlaces;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "default_currency", nullable = false)
    private boolean defaultCurrency;
}

