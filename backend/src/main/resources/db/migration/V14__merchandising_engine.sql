create table if not exists merchandising_settings (
    id binary(16) not null,
    popularity_weight double not null,
    inventory_weight double not null,
    sales_velocity_weight double not null,
    manual_boost_weight double not null,
    new_arrival_window_days integer not null,
    new_arrival_boost double not null,
    trending_boost double not null,
    best_seller_boost double not null,
    low_stock_penalty double not null,
    max_page_size integer not null,
    created_at datetime(6) not null,
    updated_at datetime(6) null,
    created_by varchar(255) null,
    primary key (id)
);

create table if not exists merchandising_boosts (
    id binary(16) not null,
    product_id binary(16) not null,
    label varchar(120) not null,
    boost_value double not null,
    active bit not null,
    start_at datetime(6) null,
    end_at datetime(6) null,
    created_at datetime(6) not null,
    updated_at datetime(6) null,
    created_by varchar(255) null,
    primary key (id),
    constraint fk_merchandising_boost_product foreign key (product_id) references products (id)
);

create index idx_merchandising_boost_product on merchandising_boosts (product_id);
create index idx_merchandising_boost_active on merchandising_boosts (active, start_at, end_at);
