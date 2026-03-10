create table if not exists recommendation_settings (
    id binary(16) not null,
    product_view_weight double not null,
    add_to_cart_weight double not null,
    checkout_weight double not null,
    trending_boost double not null,
    best_seller_boost double not null,
    rating_weight double not null,
    category_affinity_weight double not null,
    brand_affinity_weight double not null,
    co_purchase_weight double not null,
    deal_boost double not null,
    max_recommendations integer not null,
    created_at datetime(6) not null,
    updated_at datetime(6) null,
    created_by varchar(255) null,
    primary key (id)
);
