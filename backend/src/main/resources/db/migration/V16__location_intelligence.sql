-- Enterprise Location Intelligence Module (OSM-compatible)
-- Note: This migration uses MySQL-friendly types used by the enterprise modules (UUID stored as BINARY(16)).

-- ---- Addresses: add optional geo + operational fields ----
alter table addresses
    add column if not exists phone varchar(60) null;

alter table addresses
    add column if not exists line2 varchar(255) null;

alter table addresses
    add column if not exists district varchar(255) null;

alter table addresses
    add column if not exists latitude decimal(10, 7) null;

alter table addresses
    add column if not exists longitude decimal(10, 7) null;

alter table addresses
    add column if not exists accuracy_meters integer null;

alter table addresses
    add column if not exists place_id varchar(220) null;

alter table addresses
    add column if not exists formatted_address varchar(1024) null;

alter table addresses
    add column if not exists delivery_instructions varchar(600) null;

-- ---- User location captures ----
create table if not exists user_locations (
    id binary(16) not null,
    user_id binary(16) not null,
    latitude decimal(10, 7) not null,
    longitude decimal(10, 7) not null,
    accuracy_meters integer null,
    source varchar(32) not null,
    formatted_address varchar(1024) null,
    country varchar(120) null,
    region varchar(120) null,
    city varchar(120) null,
    district varchar(120) null,
    postal_code varchar(40) null,
    place_id varchar(220) null,
    captured_at datetime(6) not null,
    consent_given bit not null,
    purpose varchar(80) null,
    verified bit not null default 1,
    created_at datetime(6) not null,
    updated_at datetime(6) null,
    created_by varchar(255) null,
    primary key (id),
    constraint fk_user_locations_user foreign key (user_id) references users (id)
);

create index idx_user_locations_user_time on user_locations (user_id, captured_at);

-- ---- Service areas (radius/polygon/city/district) ----
create table if not exists service_areas (
    id binary(16) not null,
    name varchar(160) not null,
    type varchar(24) not null,
    status varchar(24) not null,
    center_latitude decimal(10, 7) null,
    center_longitude decimal(10, 7) null,
    radius_meters integer null,
    polygon_geo_json longtext null,
    rules_json longtext null,
    created_at datetime(6) not null,
    updated_at datetime(6) null,
    created_by varchar(255) null,
    primary key (id)
);

create index idx_service_areas_status_type on service_areas (status, type);

create table if not exists service_area_stores (
    service_area_id binary(16) not null,
    store_id binary(16) not null,
    primary key (service_area_id, store_id),
    constraint fk_service_area_stores_area foreign key (service_area_id) references service_areas (id) on delete cascade,
    constraint fk_service_area_stores_store foreign key (store_id) references stores (id) on delete cascade
);

create index idx_service_area_stores_store on service_area_stores (store_id);

-- ---- Photo location metadata (EXIF/manual) ----
create table if not exists photo_location_metadata (
    id binary(16) not null,
    media_id binary(16) not null,
    owner_id binary(16) null,
    latitude decimal(10, 7) null,
    longitude decimal(10, 7) null,
    captured_at datetime(6) null,
    source varchar(32) not null,
    accuracy_meters integer null,
    address_snapshot varchar(1024) null,
    privacy_level varchar(32) not null default 'INTERNAL',
    visible_to_admin bit not null default 0,
    created_at datetime(6) not null,
    updated_at datetime(6) null,
    created_by varchar(255) null,
    primary key (id),
    unique key uk_photo_location_media (media_id)
);

create index idx_photo_location_owner on photo_location_metadata (owner_id);

