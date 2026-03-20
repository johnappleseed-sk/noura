#!/usr/bin/env bash

set -euo pipefail

source "$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)/local-common.sh"

load_local_env
ensure_runtime_dirs

require_command docker

log "Starting PostgreSQL infrastructure container if needed"
docker_compose_local up -d postgres >/dev/null
wait_for_container_health noura-postgres

# These services still share one local PostgreSQL schema, so local bring-up needs the
# V1 foundations present before each service's own Flyway history can safely baseline.
declare -a foundation_migrations=(
  "carts|services/cart-service/src/main/resources/db/migration/V1__cart_foundation.sql"
  "checkout_request_records|services/checkout-service/src/main/resources/db/migration/V1__checkout_idempotency.sql"
  "customer_profiles|services/customer-service/src/main/resources/db/migration/V1__customer_profile_and_address_book.sql"
  "inventory_stock_levels|services/inventory-service/src/main/resources/db/migration/V1__inventory_stock_baseline.sql"
  "notification_messages|services/notification-service/src/main/resources/db/migration/V1__notification_service_init.sql"
  "orders|services/order-service/src/main/resources/db/migration/V1__order_foundation.sql"
  "payment_transactions|services/payment-service/src/main/resources/db/migration/V1__payment_foundation.sql"
  "pricing_currencies|services/pricing-service/src/main/resources/db/migration/V1__pricing_foundation.sql"
  "promotions|services/promotion-service/src/main/resources/db/migration/V1__promotion_foundation.sql"
  "product_reviews|services/review-service/src/main/resources/db/migration/V1__review_foundation.sql"
  "search_product_documents|services/search-service/src/main/resources/db/migration/V1__search_projection_foundation.sql"
  "shipment_records|services/shipping-service/src/main/resources/db/migration/V1__shipping_foundation.sql"
)

for migration_entry in "${foundation_migrations[@]}"; do
  table_name="${migration_entry%%|*}"
  migration_file="${migration_entry#*|}"

  if table_exists "${table_name}"; then
    log "Skipping ${table_name} foundation; table already exists"
    continue
  fi

  log "Applying foundation migration for ${table_name}"
  run_sql_file "${REPO_ROOT}/${migration_file}"
done

log "Patching inventory audit columns required by the current entity model"
run_sql "
ALTER TABLE inventory_stock_movements
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(120) NULL;

UPDATE inventory_stock_movements
SET updated_at = COALESCE(updated_at, created_at, NOW())
WHERE updated_at IS NULL;
"

log "Applying shared catalog bootstrap and demo data seed"
run_sql_file "${LOCAL_SCRIPT_DIR}/sql/local-shared-catalog-bootstrap.sql"

log "Local database bootstrap complete"
