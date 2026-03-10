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
@Table(name = "recommendation_settings")
public class RecommendationSettings extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_view_weight", nullable = false)
    private double productViewWeight = 1D;

    @Column(name = "add_to_cart_weight", nullable = false)
    private double addToCartWeight = 4D;

    @Column(name = "checkout_weight", nullable = false)
    private double checkoutWeight = 8D;

    @Column(name = "trending_boost", nullable = false)
    private double trendingBoost = 30D;

    @Column(name = "best_seller_boost", nullable = false)
    private double bestSellerBoost = 20D;

    @Column(name = "rating_weight", nullable = false)
    private double ratingWeight = 5D;

    @Column(name = "category_affinity_weight", nullable = false)
    private double categoryAffinityWeight = 6D;

    @Column(name = "brand_affinity_weight", nullable = false)
    private double brandAffinityWeight = 3D;

    @Column(name = "co_purchase_weight", nullable = false)
    private double coPurchaseWeight = 5D;

    @Column(name = "deal_boost", nullable = false)
    private double dealBoost = 60D;

    @Column(name = "max_recommendations", nullable = false)
    private int maxRecommendations = 12;
}
