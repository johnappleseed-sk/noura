-- PostgreSQL reporting/search foundation.
-- Introduces reusable analytics views, a materialized revenue snapshot,
-- and targeted indexes for common commerce query patterns.

CREATE EXTENSION IF NOT EXISTS "pg_trgm";

CREATE OR REPLACE FUNCTION fn_order_total(
    p_subtotal NUMERIC,
    p_discount NUMERIC,
    p_shipping NUMERIC
)
RETURNS NUMERIC
LANGUAGE sql
IMMUTABLE
AS $$
    SELECT COALESCE(p_subtotal, 0) - COALESCE(p_discount, 0) + COALESCE(p_shipping, 0);
$$;

CREATE OR REPLACE VIEW vw_sales_summary_daily AS
SELECT
    DATE_TRUNC('day', o.created_at)::date AS sales_day,
    COUNT(*)::bigint AS order_count,
    COALESCE(SUM(o.total_amount), 0)::NUMERIC(18, 2) AS gross_revenue,
    COALESCE(SUM(o.discount_amount), 0)::NUMERIC(18, 2) AS total_discount,
    COALESCE(SUM(o.shipping_amount), 0)::NUMERIC(18, 2) AS total_shipping,
    COALESCE(AVG(o.total_amount), 0)::NUMERIC(18, 2) AS average_order_value
FROM orders o
GROUP BY DATE_TRUNC('day', o.created_at)::date;

CREATE MATERIALIZED VIEW IF NOT EXISTS mv_revenue_analytics_daily AS
SELECT
    DATE_TRUNC('day', o.created_at)::date AS revenue_day,
    COUNT(*)::bigint AS order_count,
    COALESCE(SUM(o.total_amount), 0)::NUMERIC(18, 2) AS gross_revenue,
    COALESCE(SUM(o.discount_amount), 0)::NUMERIC(18, 2) AS discount_total,
    COALESCE(SUM(o.shipping_amount), 0)::NUMERIC(18, 2) AS shipping_total,
    COALESCE(AVG(o.total_amount), 0)::NUMERIC(18, 2) AS avg_order_value
FROM orders o
GROUP BY DATE_TRUNC('day', o.created_at)::date
WITH DATA;

CREATE UNIQUE INDEX IF NOT EXISTS ux_mv_revenue_analytics_daily_day
    ON mv_revenue_analytics_daily (revenue_day);

CREATE OR REPLACE FUNCTION fn_refresh_revenue_analytics_daily()
RETURNS VOID
LANGUAGE plpgsql
AS $$
BEGIN
    REFRESH MATERIALIZED VIEW mv_revenue_analytics_daily;
END;
$$;

CREATE OR REPLACE VIEW vw_inventory_dashboard AS
SELECT
    i.warehouse_id,
    w.name AS warehouse_name,
    i.variant_id,
    pv.sku,
    COALESCE(SUM(i.quantity), 0)::BIGINT AS quantity_on_hand,
    COALESCE(SUM(i.reserved_quantity), 0)::BIGINT AS quantity_reserved,
    COALESCE(SUM(i.quantity - i.reserved_quantity), 0)::BIGINT AS quantity_available
FROM inventory i
JOIN warehouses w ON w.id = i.warehouse_id
JOIN product_variants pv ON pv.id = i.variant_id
GROUP BY i.warehouse_id, w.name, i.variant_id, pv.sku;

CREATE INDEX IF NOT EXISTS idx_products_name_trgm
    ON products USING gin (name gin_trgm_ops);

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS search_document tsvector
    GENERATED ALWAYS AS (
        to_tsvector(
            'simple',
            COALESCE(name, '') || ' ' || COALESCE(short_description, '') || ' ' || COALESCE(long_description, '')
        )
    ) STORED;

CREATE INDEX IF NOT EXISTS idx_products_search_document_gin
    ON products USING gin (search_document);

CREATE INDEX IF NOT EXISTS idx_products_active_not_deleted
    ON products (created_at DESC)
    WHERE active = TRUE AND deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_orders_open_status
    ON orders (status, created_at DESC)
    WHERE status IN ('CREATED', 'REVIEWED', 'PAYMENT_PENDING', 'PAID');
