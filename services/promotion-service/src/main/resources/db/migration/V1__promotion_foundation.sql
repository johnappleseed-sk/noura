CREATE TABLE promotions (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(180),
    updated_by VARCHAR(180),
    name VARCHAR(180) NOT NULL,
    code VARCHAR(120),
    description VARCHAR(1000),
    type VARCHAR(64) NOT NULL,
    coupon_code VARCHAR(120),
    conditions_json TEXT NOT NULL,
    start_date TIMESTAMPTZ,
    end_date TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_stackable BOOLEAN NOT NULL DEFAULT TRUE,
    priority INTEGER NOT NULL DEFAULT 0,
    usage_limit_total INTEGER,
    usage_limit_per_customer INTEGER,
    usage_count INTEGER NOT NULL DEFAULT 0,
    customer_segment VARCHAR(120),
    is_archived BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX ux_promotions_code
    ON promotions (LOWER(code))
    WHERE code IS NOT NULL;

CREATE UNIQUE INDEX ux_promotions_coupon_code
    ON promotions (LOWER(coupon_code))
    WHERE coupon_code IS NOT NULL;

CREATE INDEX ix_promotions_active_archived_priority
    ON promotions (is_active, is_archived, priority DESC, updated_at DESC);

CREATE TABLE promotion_applications (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(180),
    updated_by VARCHAR(180),
    promotion_id UUID NOT NULL REFERENCES promotions(id) ON DELETE CASCADE,
    applicable_entity_type VARCHAR(40) NOT NULL,
    applicable_entity_id UUID NOT NULL
);

CREATE INDEX ix_promotion_applications_promotion_id
    ON promotion_applications (promotion_id);

CREATE INDEX ix_promotion_applications_entity
    ON promotion_applications (applicable_entity_type, applicable_entity_id);
