ALTER TABLE stores
    ADD COLUMN IF NOT EXISTS service_radius_meters INTEGER NULL;

ALTER TABLE addresses
    ADD COLUMN IF NOT EXISTS validation_status VARCHAR(32) NULL;

ALTER TABLE carts
    ADD COLUMN IF NOT EXISTS address_id UUID NULL;

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS address_id UUID NULL;

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS location_snapshot_json TEXT NULL;

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS matched_service_area_id UUID NULL;

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS eligibility_reason VARCHAR(80) NULL;

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS delivery_latitude DECIMAL(10, 7) NULL;

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS delivery_longitude DECIMAL(10, 7) NULL;

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS address_validation_status VARCHAR(32) NULL;

CREATE INDEX IF NOT EXISTS idx_orders_address_id ON orders (address_id);
CREATE INDEX IF NOT EXISTS idx_orders_service_area ON orders (matched_service_area_id);
