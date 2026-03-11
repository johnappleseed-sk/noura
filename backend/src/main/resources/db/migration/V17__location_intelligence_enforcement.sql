alter table stores
    add column if not exists service_radius_meters integer null;

alter table addresses
    add column if not exists validation_status varchar(32) null;

alter table carts
    add column if not exists address_id binary(16) null;

alter table orders
    add column if not exists address_id binary(16) null;

alter table orders
    add column if not exists location_snapshot_json longtext null;

alter table orders
    add column if not exists matched_service_area_id binary(16) null;

alter table orders
    add column if not exists eligibility_reason varchar(80) null;

alter table orders
    add column if not exists delivery_latitude decimal(10, 7) null;

alter table orders
    add column if not exists delivery_longitude decimal(10, 7) null;

alter table orders
    add column if not exists address_validation_status varchar(32) null;

create index idx_orders_address_id on orders (address_id);
create index idx_orders_service_area on orders (matched_service_area_id);
