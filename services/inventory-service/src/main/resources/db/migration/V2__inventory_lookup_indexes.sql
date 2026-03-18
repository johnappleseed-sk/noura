CREATE INDEX IF NOT EXISTS idx_inventory_stock_levels_product_updated_at
    ON inventory_stock_levels (product_id, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_inventory_stock_movements_product_created_at
    ON inventory_stock_movements (product_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_inventory_stock_movements_product_warehouse_created_at
    ON inventory_stock_movements (product_id, warehouse_id, created_at DESC);
