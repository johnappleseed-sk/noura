CREATE TABLE IF NOT EXISTS recommendation_settings (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    product_view_weight DOUBLE PRECISION NOT NULL,
    add_to_cart_weight DOUBLE PRECISION NOT NULL,
    checkout_weight DOUBLE PRECISION NOT NULL,
    trending_boost DOUBLE PRECISION NOT NULL,
    best_seller_boost DOUBLE PRECISION NOT NULL,
    rating_weight DOUBLE PRECISION NOT NULL,
    category_affinity_weight DOUBLE PRECISION NOT NULL,
    brand_affinity_weight DOUBLE PRECISION NOT NULL,
    co_purchase_weight DOUBLE PRECISION NOT NULL,
    deal_boost DOUBLE PRECISION NOT NULL,
    max_recommendations INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NULL,
    created_by VARCHAR(255) NULL,
    PRIMARY KEY (id)
);
