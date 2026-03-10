alter table merchandising_settings
    add column if not exists impression_weight double not null default 0.75;

alter table merchandising_settings
    add column if not exists click_weight double not null default 4.0;

alter table merchandising_settings
    add column if not exists click_through_rate_weight double not null default 0.6;
