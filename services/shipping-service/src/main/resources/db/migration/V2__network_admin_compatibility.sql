CREATE TABLE merchant_records (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(180),
    updated_by VARCHAR(180),
    merchant_code VARCHAR(64) NOT NULL,
    legal_name VARCHAR(180) NOT NULL,
    display_name VARCHAR(180) NOT NULL,
    email VARCHAR(180),
    phone VARCHAR(64),
    country_code VARCHAR(16),
    status VARCHAR(32) NOT NULL,
    contract_start_at TIMESTAMPTZ,
    contract_end_at TIMESTAMPTZ,
    notes VARCHAR(2000)
);

CREATE UNIQUE INDEX ux_merchant_records_code
    ON merchant_records (merchant_code);

CREATE TABLE store_records (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(180),
    updated_by VARCHAR(180),
    store_code VARCHAR(64) NOT NULL,
    name VARCHAR(180) NOT NULL,
    slug VARCHAR(180),
    merchant_id UUID,
    type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    country_code VARCHAR(16),
    city VARCHAR(120),
    address_line_1 VARCHAR(240),
    address_line_2 VARCHAR(240),
    postal_code VARCHAR(32),
    contact_email VARCHAR(180),
    contact_phone VARCHAR(64),
    latitude NUMERIC(12, 8),
    longitude NUMERIC(12, 8),
    open_now BOOLEAN NOT NULL DEFAULT TRUE,
    preferred_store BOOLEAN NOT NULL DEFAULT FALSE,
    supported_services_json JSONB,
    deleted_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX ux_store_records_code
    ON store_records (store_code);

CREATE INDEX ix_store_records_status_deleted
    ON store_records (status, deleted_at);

CREATE INDEX ix_store_records_merchant_id
    ON store_records (merchant_id);

CREATE TABLE service_area_records (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(180),
    updated_by VARCHAR(180),
    name VARCHAR(180) NOT NULL,
    type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    center_latitude NUMERIC(12, 8),
    center_longitude NUMERIC(12, 8),
    radius_meters INTEGER,
    polygon_geo_json TEXT,
    rules_json TEXT,
    store_ids_json JSONB,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX ix_service_area_records_status_deleted
    ON service_area_records (status, deleted_at);
