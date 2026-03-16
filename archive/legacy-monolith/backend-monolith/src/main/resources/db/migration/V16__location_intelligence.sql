-- Enterprise Location Intelligence Module (OSM-compatible)

-- ---- Addresses: add optional geo + operational fields ----
ALTER TABLE addresses
    ADD COLUMN IF NOT EXISTS phone VARCHAR(60) NULL;

ALTER TABLE addresses
    ADD COLUMN IF NOT EXISTS line2 VARCHAR(255) NULL;

ALTER TABLE addresses
    ADD COLUMN IF NOT EXISTS district VARCHAR(255) NULL;

ALTER TABLE addresses
    ADD COLUMN IF NOT EXISTS latitude DECIMAL(10, 7) NULL;

ALTER TABLE addresses
    ADD COLUMN IF NOT EXISTS longitude DECIMAL(10, 7) NULL;

ALTER TABLE addresses
    ADD COLUMN IF NOT EXISTS accuracy_meters INTEGER NULL;

ALTER TABLE addresses
    ADD COLUMN IF NOT EXISTS place_id VARCHAR(220) NULL;

ALTER TABLE addresses
    ADD COLUMN IF NOT EXISTS formatted_address VARCHAR(1024) NULL;

ALTER TABLE addresses
    ADD COLUMN IF NOT EXISTS delivery_instructions VARCHAR(600) NULL;

-- ---- User location captures ----
CREATE TABLE IF NOT EXISTS user_locations (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    latitude DECIMAL(10, 7) NOT NULL,
    longitude DECIMAL(10, 7) NOT NULL,
    accuracy_meters INTEGER NULL,
    source VARCHAR(32) NOT NULL,
    formatted_address VARCHAR(1024) NULL,
    country VARCHAR(120) NULL,
    region VARCHAR(120) NULL,
    city VARCHAR(120) NULL,
    district VARCHAR(120) NULL,
    postal_code VARCHAR(40) NULL,
    place_id VARCHAR(220) NULL,
    captured_at TIMESTAMPTZ NOT NULL,
    consent_given BOOLEAN NOT NULL,
    purpose VARCHAR(80) NULL,
    verified BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NULL,
    created_by VARCHAR(255) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_user_locations_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_user_locations_user_time ON user_locations (user_id, captured_at);

-- ---- Service areas (radius/polygon/city/district) ----
CREATE TABLE IF NOT EXISTS service_areas (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    name VARCHAR(160) NOT NULL,
    type VARCHAR(24) NOT NULL,
    status VARCHAR(24) NOT NULL,
    center_latitude DECIMAL(10, 7) NULL,
    center_longitude DECIMAL(10, 7) NULL,
    radius_meters INTEGER NULL,
    polygon_geo_json TEXT NULL,
    rules_json TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NULL,
    created_by VARCHAR(255) NULL,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_service_areas_status_type ON service_areas (status, type);

CREATE TABLE IF NOT EXISTS service_area_stores (
    service_area_id UUID NOT NULL,
    store_id UUID NOT NULL,
    PRIMARY KEY (service_area_id, store_id),
    CONSTRAINT fk_service_area_stores_area FOREIGN KEY (service_area_id) REFERENCES service_areas (id) ON DELETE CASCADE,
    CONSTRAINT fk_service_area_stores_store FOREIGN KEY (store_id) REFERENCES stores (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_service_area_stores_store ON service_area_stores (store_id);

-- ---- Photo location metadata (EXIF/manual) ----
CREATE TABLE IF NOT EXISTS photo_location_metadata (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    media_id UUID NOT NULL,
    owner_id UUID NULL,
    latitude DECIMAL(10, 7) NULL,
    longitude DECIMAL(10, 7) NULL,
    captured_at TIMESTAMPTZ NULL,
    source VARCHAR(32) NOT NULL,
    accuracy_meters INTEGER NULL,
    address_snapshot VARCHAR(1024) NULL,
    privacy_level VARCHAR(32) NOT NULL DEFAULT 'INTERNAL',
    visible_to_admin BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NULL,
    created_by VARCHAR(255) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_photo_location_media UNIQUE (media_id)
);

CREATE INDEX IF NOT EXISTS idx_photo_location_owner ON photo_location_metadata (owner_id);
