package com.noura.platform.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "merchandising_settings")
public class MerchandisingSettings extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "popularity_weight", nullable = false)
    private double popularityWeight = 1D;

    @Column(name = "inventory_weight", nullable = false)
    private double inventoryWeight = 0.5D;

    @Column(name = "impression_weight", nullable = false)
    private double impressionWeight = 0.75D;

    @Column(name = "click_weight", nullable = false)
    private double clickWeight = 4D;

    @Column(name = "click_through_rate_weight", nullable = false)
    private double clickThroughRateWeight = 0.6D;

    @Column(name = "manual_boost_weight", nullable = false)
    private double manualBoostWeight = 1D;

    @Column(name = "new_arrival_window_days", nullable = false)
    private int newArrivalWindowDays = 30;

    @Column(name = "new_arrival_boost", nullable = false)
    private double newArrivalBoost = 25D;

    @Column(name = "trending_boost", nullable = false)
    private double trendingBoost = 20D;

    @Column(name = "best_seller_boost", nullable = false)
    private double bestSellerBoost = 15D;

    @Column(name = "low_stock_penalty", nullable = false)
    private double lowStockPenalty = 20D;

    @Column(name = "max_page_size", nullable = false)
    private int maxPageSize = 48;
}
