INSERT INTO coupons (code, discount_percent, free_shipping, min_order_value, active, created_at, created_by)
VALUES
    ('SAVE10', 10, FALSE, 0, TRUE, NOW(), 'flyway'),
    ('PREMIUM15', 15, FALSE, 100, TRUE, NOW(), 'flyway'),
    ('FREESHIP5', 5, TRUE, 0, TRUE, NOW(), 'flyway')
ON CONFLICT (code) DO NOTHING;

INSERT INTO trend_tags (value, score, created_at, created_by)
VALUES
    ('flash-sale', 97, NOW(), 'flyway'),
    ('b2b-deals', 89, NOW(), 'flyway'),
    ('trending-now', 93, NOW(), 'flyway'),
    ('new-arrivals', 82, NOW(), 'flyway'),
    ('top-rated', 87, NOW(), 'flyway')
ON CONFLICT (value) DO NOTHING;
