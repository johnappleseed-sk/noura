CREATE TABLE IF NOT EXISTS merchandising_settings (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    popularity_weight DOUBLE PRECISION NOT NULL,
    inventory_weight DOUBLE PRECISION NOT NULL,
    sales_velocity_weight DOUBLE PRECISION NOT NULL DEFAULT 0,
    manual_boost_weight DOUBLE PRECISION NOT NULL,
    new_arrival_window_days INTEGER NOT NULL,
    new_arrival_boost DOUBLE PRECISION NOT NULL,
    trending_boost DOUBLE PRECISION NOT NULL,
    best_seller_boost DOUBLE PRECISION NOT NULL,
    low_stock_penalty DOUBLE PRECISION NOT NULL,
    max_page_size INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NULL,
    created_by VARCHAR(255) NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS merchandising_boosts (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL,
    label VARCHAR(120) NOT NULL,
    boost_value DOUBLE PRECISION NOT NULL,
    active BOOLEAN NOT NULL,
    start_at TIMESTAMPTZ NULL,
    end_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NULL,
    created_by VARCHAR(255) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_merchandising_boost_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE INDEX IF NOT EXISTS idx_merchandising_boost_product ON merchandising_boosts (product_id);
CREATE INDEX IF NOT EXISTS idx_merchandising_boost_active ON merchandising_boosts (active, start_at, end_at);
