ALTER TABLE cart_items
    ADD COLUMN IF NOT EXISTS store_product_reference_id UUID;

INSERT INTO store_product_references (id, store_id, product_id, active, created_at, created_by)
SELECT gen_random_uuid(), source.store_id, source.product_id, TRUE, NOW(), 'flyway'
FROM (
    SELECT DISTINCT c.store_id, ci.product_id
    FROM cart_items ci
    JOIN carts c ON c.id = ci.cart_id
    WHERE c.store_id IS NOT NULL
) source
LEFT JOIN store_product_references spr
    ON spr.store_id = source.store_id
   AND spr.product_id = source.product_id
WHERE spr.id IS NULL;

UPDATE cart_items ci
SET store_product_reference_id = spr.id
FROM carts c
JOIN store_product_references spr
    ON spr.store_id = c.store_id
   AND spr.product_id = ci.product_id
WHERE c.id = ci.cart_id
  AND c.store_id IS NOT NULL
  AND ci.store_product_reference_id IS NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_cart_items_store_product_reference'
    ) THEN
        ALTER TABLE cart_items
            ADD CONSTRAINT fk_cart_items_store_product_reference
            FOREIGN KEY (store_product_reference_id)
            REFERENCES store_product_references (id)
            ON DELETE RESTRICT;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_cart_items_quantity_positive'
    ) THEN
        ALTER TABLE cart_items
            ADD CONSTRAINT ck_cart_items_quantity_positive
            CHECK (quantity > 0);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_carts_user_id
    ON carts (user_id);

CREATE INDEX IF NOT EXISTS idx_cart_items_cart_id_created_at
    ON cart_items (cart_id, created_at);

CREATE INDEX IF NOT EXISTS idx_cart_items_store_product_reference_id
    ON cart_items (store_product_reference_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_cart_items_cart_store_product_reference
    ON cart_items (cart_id, store_product_reference_id)
    WHERE store_product_reference_id IS NOT NULL;
