CREATE TABLE IF NOT EXISTS iam_permissions (
    id CHAR(36) NOT NULL,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_permissions_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS iam_roles (
    id CHAR(36) NOT NULL,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500) NULL,
    system_role BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_roles_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS iam_role_permissions (
    role_id CHAR(36) NOT NULL,
    permission_id CHAR(36) NOT NULL,
    granted_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_iam_role_permissions_role
        FOREIGN KEY (role_id) REFERENCES iam_roles (id),
    CONSTRAINT fk_iam_role_permissions_permission
        FOREIGN KEY (permission_id) REFERENCES iam_permissions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS iam_users (
    id CHAR(36) NOT NULL,
    username VARCHAR(120) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(180) NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'ACTIVE',
    last_login_at DATETIME(6) NULL,
    deleted_at DATETIME(6) NULL,
    archived_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_users_username (username),
    UNIQUE KEY uk_iam_users_email (email),
    KEY idx_iam_users_status (status),
    KEY idx_iam_users_deleted_at (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS iam_user_roles (
    user_id CHAR(36) NOT NULL,
    role_id CHAR(36) NOT NULL,
    assigned_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    assigned_by CHAR(36) NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_iam_user_roles_user
        FOREIGN KEY (user_id) REFERENCES iam_users (id),
    CONSTRAINT fk_iam_user_roles_role
        FOREIGN KEY (role_id) REFERENCES iam_roles (id),
    CONSTRAINT fk_iam_user_roles_assigned_by
        FOREIGN KEY (assigned_by) REFERENCES iam_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS categories (
    id CHAR(36) NOT NULL,
    parent_id CHAR(36) NULL,
    category_code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    level INT NOT NULL DEFAULT 0,
    sort_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at DATETIME(6) NULL,
    archived_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_categories_code (category_code),
    KEY idx_categories_parent_id (parent_id),
    KEY idx_categories_active (active),
    CONSTRAINT fk_categories_parent
        FOREIGN KEY (parent_id) REFERENCES categories (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS products (
    id CHAR(36) NOT NULL,
    sku VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'DRAFT',
    base_price DECIMAL(18, 4) NOT NULL,
    currency_code CHAR(3) NOT NULL DEFAULT 'USD',
    width_cm DECIMAL(18, 4) NULL,
    height_cm DECIMAL(18, 4) NULL,
    length_cm DECIMAL(18, 4) NULL,
    weight_kg DECIMAL(18, 4) NULL,
    batch_tracked BOOLEAN NOT NULL DEFAULT FALSE,
    serial_tracked BOOLEAN NOT NULL DEFAULT FALSE,
    barcode_value VARCHAR(255) NULL,
    qr_code_value VARCHAR(255) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at DATETIME(6) NULL,
    archived_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_products_sku (sku),
    KEY idx_products_status (status),
    KEY idx_products_active (active),
    KEY idx_products_deleted_at (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS product_categories (
    product_id CHAR(36) NOT NULL,
    category_id CHAR(36) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (product_id, category_id),
    KEY idx_product_categories_category_id (category_id),
    CONSTRAINT fk_product_categories_product
        FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_product_categories_category
        FOREIGN KEY (category_id) REFERENCES categories (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS warehouses (
    id CHAR(36) NOT NULL,
    warehouse_code VARCHAR(80) NOT NULL,
    name VARCHAR(255) NOT NULL,
    warehouse_type VARCHAR(60) NOT NULL DEFAULT 'FULFILLMENT',
    address_line_1 VARCHAR(255) NULL,
    address_line_2 VARCHAR(255) NULL,
    city VARCHAR(120) NULL,
    state_province VARCHAR(120) NULL,
    postal_code VARCHAR(40) NULL,
    country_code CHAR(2) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at DATETIME(6) NULL,
    archived_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_warehouses_code (warehouse_code),
    KEY idx_warehouses_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS warehouse_bins (
    id CHAR(36) NOT NULL,
    warehouse_id CHAR(36) NOT NULL,
    bin_code VARCHAR(100) NOT NULL,
    zone_code VARCHAR(80) NULL,
    aisle_code VARCHAR(80) NULL,
    shelf_code VARCHAR(80) NULL,
    bin_type VARCHAR(60) NOT NULL DEFAULT 'STANDARD',
    barcode_value VARCHAR(255) NULL,
    qr_code_value VARCHAR(255) NULL,
    pick_sequence INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at DATETIME(6) NULL,
    archived_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_warehouse_bins_warehouse_code (warehouse_id, bin_code),
    KEY idx_warehouse_bins_zone_code (zone_code),
    KEY idx_warehouse_bins_active (active),
    CONSTRAINT fk_warehouse_bins_warehouse
        FOREIGN KEY (warehouse_id) REFERENCES warehouses (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stock_policies (
    id CHAR(36) NOT NULL,
    product_id CHAR(36) NOT NULL,
    warehouse_id CHAR(36) NULL,
    warehouse_scope CHAR(36) GENERATED ALWAYS AS (
        IFNULL(warehouse_id, '00000000-0000-0000-0000-000000000000')
    ) STORED,
    low_stock_threshold DECIMAL(18, 4) NOT NULL DEFAULT 0,
    reorder_point DECIMAL(18, 4) NOT NULL DEFAULT 0,
    reorder_quantity DECIMAL(18, 4) NOT NULL DEFAULT 0,
    max_stock_level DECIMAL(18, 4) NULL,
    allow_backorder BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_policies_scope (product_id, warehouse_scope),
    CONSTRAINT fk_stock_policies_product
        FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_stock_policies_warehouse
        FOREIGN KEY (warehouse_id) REFERENCES warehouses (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS batch_lots (
    id CHAR(36) NOT NULL,
    product_id CHAR(36) NOT NULL,
    lot_number VARCHAR(120) NOT NULL,
    supplier_batch_ref VARCHAR(120) NULL,
    manufactured_at DATE NULL,
    received_at DATETIME(6) NULL,
    expiry_date DATE NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'ACTIVE',
    notes VARCHAR(500) NULL,
    deleted_at DATETIME(6) NULL,
    archived_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_batch_lots_product_lot_number (product_id, lot_number),
    KEY idx_batch_lots_expiry_date (expiry_date),
    KEY idx_batch_lots_status (status),
    CONSTRAINT fk_batch_lots_product
        FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stock_levels (
    id CHAR(36) NOT NULL,
    product_id CHAR(36) NOT NULL,
    warehouse_id CHAR(36) NOT NULL,
    bin_id CHAR(36) NULL,
    batch_id CHAR(36) NULL,
    bin_scope CHAR(36) GENERATED ALWAYS AS (
        IFNULL(bin_id, '00000000-0000-0000-0000-000000000000')
    ) STORED,
    batch_scope CHAR(36) GENERATED ALWAYS AS (
        IFNULL(batch_id, '00000000-0000-0000-0000-000000000000')
    ) STORED,
    quantity_on_hand DECIMAL(18, 4) NOT NULL DEFAULT 0,
    quantity_reserved DECIMAL(18, 4) NOT NULL DEFAULT 0,
    quantity_available DECIMAL(18, 4) NOT NULL DEFAULT 0,
    quantity_damaged DECIMAL(18, 4) NOT NULL DEFAULT 0,
    last_movement_at DATETIME(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_levels_scope (product_id, warehouse_id, bin_scope, batch_scope),
    KEY idx_stock_levels_warehouse_product (warehouse_id, product_id),
    KEY idx_stock_levels_low_available (quantity_available),
    CONSTRAINT fk_stock_levels_product
        FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_stock_levels_warehouse
        FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_stock_levels_bin
        FOREIGN KEY (bin_id) REFERENCES warehouse_bins (id),
    CONSTRAINT fk_stock_levels_batch
        FOREIGN KEY (batch_id) REFERENCES batch_lots (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stock_movements (
    id CHAR(36) NOT NULL,
    movement_number VARCHAR(100) NOT NULL,
    movement_type VARCHAR(40) NOT NULL,
    movement_status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    source_warehouse_id CHAR(36) NULL,
    source_bin_id CHAR(36) NULL,
    destination_warehouse_id CHAR(36) NULL,
    destination_bin_id CHAR(36) NULL,
    reference_type VARCHAR(60) NULL,
    reference_id VARCHAR(120) NULL,
    external_reference VARCHAR(120) NULL,
    notes VARCHAR(1000) NULL,
    processed_by CHAR(36) NULL,
    processed_at DATETIME(6) NULL,
    deleted_at DATETIME(6) NULL,
    archived_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_movements_movement_number (movement_number),
    KEY idx_stock_movements_type_status (movement_type, movement_status),
    KEY idx_stock_movements_processed_at (processed_at),
    CONSTRAINT fk_stock_movements_source_warehouse
        FOREIGN KEY (source_warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_stock_movements_source_bin
        FOREIGN KEY (source_bin_id) REFERENCES warehouse_bins (id),
    CONSTRAINT fk_stock_movements_destination_warehouse
        FOREIGN KEY (destination_warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_stock_movements_destination_bin
        FOREIGN KEY (destination_bin_id) REFERENCES warehouse_bins (id),
    CONSTRAINT fk_stock_movements_processed_by
        FOREIGN KEY (processed_by) REFERENCES iam_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stock_movement_lines (
    id CHAR(36) NOT NULL,
    movement_id CHAR(36) NOT NULL,
    line_number INT NOT NULL,
    product_id CHAR(36) NOT NULL,
    batch_id CHAR(36) NULL,
    from_bin_id CHAR(36) NULL,
    to_bin_id CHAR(36) NULL,
    quantity DECIMAL(18, 4) NOT NULL,
    unit_cost DECIMAL(18, 4) NULL,
    expiry_date DATE NULL,
    notes VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_movement_lines_movement_line (movement_id, line_number),
    KEY idx_stock_movement_lines_product_id (product_id),
    KEY idx_stock_movement_lines_batch_id (batch_id),
    CONSTRAINT fk_stock_movement_lines_movement
        FOREIGN KEY (movement_id) REFERENCES stock_movements (id),
    CONSTRAINT fk_stock_movement_lines_product
        FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_stock_movement_lines_batch
        FOREIGN KEY (batch_id) REFERENCES batch_lots (id),
    CONSTRAINT fk_stock_movement_lines_from_bin
        FOREIGN KEY (from_bin_id) REFERENCES warehouse_bins (id),
    CONSTRAINT fk_stock_movement_lines_to_bin
        FOREIGN KEY (to_bin_id) REFERENCES warehouse_bins (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS serial_numbers (
    id CHAR(36) NOT NULL,
    product_id CHAR(36) NOT NULL,
    batch_id CHAR(36) NULL,
    warehouse_id CHAR(36) NULL,
    bin_id CHAR(36) NULL,
    serial_number VARCHAR(180) NOT NULL,
    serial_status VARCHAR(40) NOT NULL DEFAULT 'IN_STOCK',
    last_movement_line_id CHAR(36) NULL,
    deleted_at DATETIME(6) NULL,
    archived_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_serial_numbers_serial_number (serial_number),
    KEY idx_serial_numbers_product_status (product_id, serial_status),
    KEY idx_serial_numbers_warehouse_bin (warehouse_id, bin_id),
    CONSTRAINT fk_serial_numbers_product
        FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_serial_numbers_batch
        FOREIGN KEY (batch_id) REFERENCES batch_lots (id),
    CONSTRAINT fk_serial_numbers_warehouse
        FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_serial_numbers_bin
        FOREIGN KEY (bin_id) REFERENCES warehouse_bins (id),
    CONSTRAINT fk_serial_numbers_last_movement_line
        FOREIGN KEY (last_movement_line_id) REFERENCES stock_movement_lines (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS reorder_alerts (
    id CHAR(36) NOT NULL,
    product_id CHAR(36) NOT NULL,
    warehouse_id CHAR(36) NULL,
    bin_id CHAR(36) NULL,
    stock_policy_id CHAR(36) NULL,
    current_quantity DECIMAL(18, 4) NOT NULL,
    threshold_quantity DECIMAL(18, 4) NOT NULL,
    alert_status VARCHAR(40) NOT NULL DEFAULT 'OPEN',
    acknowledged_by CHAR(36) NULL,
    acknowledged_at DATETIME(6) NULL,
    resolved_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_reorder_alerts_status_created (alert_status, created_at),
    CONSTRAINT fk_reorder_alerts_product
        FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_reorder_alerts_warehouse
        FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_reorder_alerts_bin
        FOREIGN KEY (bin_id) REFERENCES warehouse_bins (id),
    CONSTRAINT fk_reorder_alerts_policy
        FOREIGN KEY (stock_policy_id) REFERENCES stock_policies (id),
    CONSTRAINT fk_reorder_alerts_acknowledged_by
        FOREIGN KEY (acknowledged_by) REFERENCES iam_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS webhook_subscriptions (
    id CHAR(36) NOT NULL,
    event_code VARCHAR(120) NOT NULL,
    endpoint_url VARCHAR(1000) NOT NULL,
    secret_token VARCHAR(255) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    timeout_ms INT NOT NULL DEFAULT 5000,
    retry_count INT NOT NULL DEFAULT 3,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_webhook_subscriptions_event_active (event_code, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS data_exchange_jobs (
    id CHAR(36) NOT NULL,
    job_type VARCHAR(40) NOT NULL,
    entity_type VARCHAR(60) NOT NULL,
    file_format VARCHAR(20) NOT NULL,
    storage_path VARCHAR(1000) NULL,
    requested_by CHAR(36) NULL,
    job_status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    started_at DATETIME(6) NULL,
    completed_at DATETIME(6) NULL,
    error_message TEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_data_exchange_jobs_status_created (job_status, created_at),
    CONSTRAINT fk_data_exchange_jobs_requested_by
        FOREIGN KEY (requested_by) REFERENCES iam_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS audit_logs (
    id CHAR(36) NOT NULL,
    actor_user_id CHAR(36) NULL,
    actor_email VARCHAR(255) NULL,
    action_code VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id CHAR(36) NOT NULL,
    correlation_id VARCHAR(120) NULL,
    before_state_json JSON NULL,
    after_state_json JSON NULL,
    metadata_json JSON NULL,
    ip_address VARCHAR(64) NULL,
    user_agent VARCHAR(1000) NULL,
    event_hash CHAR(64) NOT NULL,
    occurred_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_audit_logs_event_hash (event_hash),
    KEY idx_audit_logs_entity_occurred (entity_type, entity_id, occurred_at),
    KEY idx_audit_logs_actor_occurred (actor_user_id, occurred_at),
    CONSTRAINT fk_audit_logs_actor_user
        FOREIGN KEY (actor_user_id) REFERENCES iam_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO iam_roles (id, code, name, description, system_role)
SELECT UUID(), 'ADMIN', 'Administrator', 'Full platform access for inventory administration.', TRUE
WHERE NOT EXISTS (SELECT 1 FROM iam_roles WHERE code = 'ADMIN');

INSERT INTO iam_roles (id, code, name, description, system_role)
SELECT UUID(), 'WAREHOUSE_MANAGER', 'Warehouse Manager', 'Operational access to warehouses, bins, and stock movements.', TRUE
WHERE NOT EXISTS (SELECT 1 FROM iam_roles WHERE code = 'WAREHOUSE_MANAGER');

INSERT INTO iam_roles (id, code, name, description, system_role)
SELECT UUID(), 'VIEWER', 'Viewer', 'Read-only access to inventory and reporting screens.', TRUE
WHERE NOT EXISTS (SELECT 1 FROM iam_roles WHERE code = 'VIEWER');

INSERT INTO iam_permissions (id, code, name, description)
SELECT UUID(), 'inventory.read', 'Read inventory', 'View products, stock levels, movements, and reports.'
WHERE NOT EXISTS (SELECT 1 FROM iam_permissions WHERE code = 'inventory.read');

INSERT INTO iam_permissions (id, code, name, description)
SELECT UUID(), 'inventory.write', 'Write inventory', 'Create and update products, warehouses, bins, and stock levels.'
WHERE NOT EXISTS (SELECT 1 FROM iam_permissions WHERE code = 'inventory.write');

INSERT INTO iam_permissions (id, code, name, description)
SELECT UUID(), 'inventory.move', 'Move stock', 'Process inbound, outbound, transfer, adjustment, and return movements.'
WHERE NOT EXISTS (SELECT 1 FROM iam_permissions WHERE code = 'inventory.move');

INSERT INTO iam_permissions (id, code, name, description)
SELECT UUID(), 'inventory.audit', 'Audit inventory', 'View immutable audit history and webhook delivery state.'
WHERE NOT EXISTS (SELECT 1 FROM iam_permissions WHERE code = 'inventory.audit');
