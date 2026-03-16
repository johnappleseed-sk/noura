# PostgreSQL Primary Migration

## Connection Targets

- Platform datasource: `jdbc:postgresql://localhost:5432/noura`
- Inventory datasource: `jdbc:postgresql://localhost:5432/noura?currentSchema=inventory`
- Environment URL format: `DATABASE_URL=postgresql://saturn:123qweasdzxc!@localhost:5432/noura`

## Data Model Strategy

- Primary platform schema remains `public`.
- Inventory persistence unit is isolated in PostgreSQL schema `inventory`.
- UUID is used as the primary key format across platform modules.
- Flexible payloads use `JSONB` on platform analytics/catalog tables.
- Media binaries remain in filesystem/object storage; PostgreSQL stores metadata references only.

## PostgreSQL Features Added

- Extensions: `pgcrypto`, `pg_trgm`
- Views:
  - `vw_sales_summary_daily`
  - `vw_inventory_dashboard`
- Materialized view:
  - `mv_revenue_analytics_daily`
- Helper functions:
  - `fn_order_total(...)`
  - `fn_refresh_revenue_analytics_daily()`
- Indexing:
  - Trigram + FTS support for product search
  - Partial index for active products
  - Partial index for open-order status analytics
  - JSONB GIN indexes for analytics/audit metadata

## Flyway Migration Notes

- MySQL-specific migrations were converted to PostgreSQL syntax in:
  - `db/migration/V13`..`V18`
  - `db/migration/pos/V10`..`V14`
  - `db/inventory/migration/V1` and `V2`
- New PostgreSQL analytics/search migration:
  - `db/migration/V20__postgres_reporting_and_search_foundation.sql`

## Rollback Guidance

- Revert application profile to previous local profile if needed.
- Restore prior migration SQL files and run `flyway repair` only when checksum history has already been applied and verified.
- Roll back PostgreSQL DDL via backup restore for production incidents (preferred over ad-hoc down-scripts).
