ALTER TABLE merchandising_settings
    ADD COLUMN IF NOT EXISTS impression_weight DOUBLE PRECISION NOT NULL DEFAULT 0.75;

ALTER TABLE merchandising_settings
    ADD COLUMN IF NOT EXISTS click_weight DOUBLE PRECISION NOT NULL DEFAULT 4.0;

ALTER TABLE merchandising_settings
    ADD COLUMN IF NOT EXISTS click_through_rate_weight DOUBLE PRECISION NOT NULL DEFAULT 0.6;
