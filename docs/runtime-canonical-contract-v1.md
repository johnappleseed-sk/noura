# Runtime Canonical Contract (Version 1.0 Foundation)

## Phase 1 execution artifacts
- `docs/v1.0-phase1-contract-catalog.md`
- `docs/v1.0-phase1-canonical-endpoint-map.md`
- `docs/v1.0-phase1-deprecation-map.md`
- `docs/v1.0-phase1-actionable-issue-list.md`

## Purpose
Define the active enterprise contract used by the current admin dashboard and storefront runtime, and explicitly mark legacy endpoints that are not part of the default execution profile.

## Active API Families
- `platform-commerce`: `/api/v1/*` (canonical for admin + storefront)
- `inventory-ops`: `/api/inventory/v1/*` (canonical for warehouse operations)

## Legacy / Non-Canonical in Default Runtime
- `legacy-storefront`: `/api/storefront/v1/*` (profile-gated, disabled by default)
- `commerce-pos-b2b`: `/commerce/api/v1/*` and related `com.noura.platform.commerce.*` APIs not consumed by current admin/storefront apps

## Capability Contract
The admin frontend now consumes:
- `GET /api/v1/admin/capabilities`
- `GET /api/v1/runtime/features`

Response shape:
- `roles`: normalized role list for the current user
- `capabilities`: map of capability key -> boolean
- `features`: runtime feature-availability map (storefront and operational toggles)

Current capability keys:
- `overview.dashboard`
- `overview.analytics`
- `commerce.catalog`
- `commerce.carousels`
- `commerce.recommendations`
- `commerce.merchandising`
- `commerce.orders`
- `commerce.returns`
- `commerce.stores`
- `commerce.pricing`
- `commerce.users`
- `commerce.notifications`
- `warehouse.catalog`
- `warehouse.locations`
- `warehouse.stock`
- `warehouse.stock.adjust`
- `warehouse.movements`
- `warehouse.batches`
- `warehouse.serials`
- `warehouse.reports`
- `warehouse.webhooks`
- `warehouse.auditLogs`
- `tools.controlCenter`
- `tools.productGenerator`

## UI Routing Policy
- Base admin access: `ADMIN`, `WAREHOUSE_MANAGER`, `VIEWER`
- Commerce and tools routes: capability-gated (admin capabilities only by default)
- Warehouse routes: capability-gated for operational roles
- Warehouse admin routes (`webhooks`, `audit-logs`): admin capability + role check

## Migration Safety Policy
Startup validator behavior:
- Non-local profiles (`!local`, `!local-mysql`, `!test`):
  - Fail if `spring.jpa.hibernate.ddl-auto` is `update`, `create`, or `create-drop`
  - Fail if `spring.flyway.enabled=false`
- Local profiles:
  - Warn on unsafe ddl modes instead of failing

This policy is enforced by `SchemaSafetyStartupValidator`.
CI enforcement:
- `.github/workflows/backend-schema-policy.yml`
- `backend/scripts/check_schema_policy.py`
