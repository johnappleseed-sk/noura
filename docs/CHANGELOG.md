## 2026-03-17

### Added
- Extracted `payment-service` application layer for internal payment intent creation, confirm flows, lookup APIs, and webhook-ready status handling.
- New pluggable payment provider boundary with deterministic sandbox/mock adapter for startup-safe integration.
- New payment lifecycle model with explicit authorization/capture sub-state tracking.
- New payment webhook delivery table and idempotent provider-event deduplication migration: `services/payment-service/src/main/resources/db/migration/V2__payment_lifecycle_and_webhooks.sql`.
- New payment API tests for request-context resolution, sandbox provider behavior, idempotent create flow, manual status transitions, and webhook processing.
- New payment documentation:
  - `docs/api/payment-service.md`
  - `docs/architecture/payment-service.md`

### Updated
- `services/payment-service/README.md` with lifecycle, webhook, sandbox scenario, and environment-variable guidance.
- `services/README.md`, root `README.md`, and `docs/backend-api.md` to reflect `payment-service` extraction.
- Payment service application config now supports:
  - `ORDER_SERVICE_INTERNAL_API_KEY`
  - `PAYMENT_SANDBOX_WEBHOOK_SECRET`

## 2026-03-13

### Added
- Admin authorization matrix API endpoint: `GET /api/v1/admin/authorization/matrix`.
- Centralized backend RBAC policy service (`AdminAuthorizationService`) with versioned role-permission matrix contract.
- New admin Roles & Permissions page at `/admin/governance/roles-permissions`.
- New admin API client endpoint: `getAdminAuthorizationMatrix`.
- Persistent RBAC schema migration `V20__admin_rbac_persistent_storage.sql` with seeded enterprise role/permission catalogs.
- Backend admin authorization management APIs:
  - `GET /api/v1/admin/authorization/permissions`
  - `GET /api/v1/admin/authorization/roles`
  - `POST /api/v1/admin/authorization/roles`
  - `PATCH /api/v1/admin/authorization/roles/{roleId}`
  - `PUT /api/v1/admin/authorization/roles/{roleId}/permissions`
  - `DELETE /api/v1/admin/authorization/roles/{roleId}`
  - `GET /api/v1/admin/authorization/users/{userId}/roles`
  - `PUT /api/v1/admin/authorization/users/{userId}/roles`
- New backend service layer `AdminRoleManagementService` for role CRUD + assignment workflows.
- Security test coverage for RBAC management controller (`AdminAuthorizationControllerSecurityIntegrationTest`).
- RBAC governance audit-log persistence (`admin_rbac_audit_logs`) via migration `V21__admin_rbac_audit_logs.sql`.
- New RBAC audit-log endpoint: `GET /api/v1/admin/authorization/audit-logs`.
- New RBAC audit-log CSV export endpoint: `GET /api/v1/admin/authorization/audit-logs/export`.
- RBAC audit-log integrity controls migration `V22__admin_rbac_audit_integrity_controls.sql` (`payload_hash` backfill + immutable update/delete trigger policy).
- New RBAC permission preset endpoint: `GET /api/v1/admin/authorization/permission-presets`.
- New RBAC preset apply endpoint: `PUT /api/v1/admin/authorization/roles/{roleId}/permission-presets/{presetCode}`.
- New RBAC bulk user-role assignment endpoint: `PUT /api/v1/admin/authorization/users/roles/bulk`.
- New RBAC bulk user-role preview endpoint: `POST /api/v1/admin/authorization/users/roles/bulk/preview`.
- New RBAC bulk saved-view endpoints:
  - `GET /api/v1/admin/authorization/users/roles/bulk/views`
  - `POST /api/v1/admin/authorization/users/roles/bulk/views`
  - `DELETE /api/v1/admin/authorization/users/roles/bulk/views/{viewId}`
- New DTO contracts for RBAC presets and bulk user-role assignment workflows.
- Persistent bulk saved-view schema migration `V24__admin_rbac_bulk_assignment_saved_views.sql`.
- New docs:
  - `docs/features/rbac-authorization-matrix.md`
  - `docs/api/admin-authorization-matrix.md`
  - `docs/frontend/roles-permissions-page.md`
  - `docs/architecture/rbac-foundation.md`
  - `docs/database/rbac-modeling.md`
  - `docs/onboarding.md`
- Storefront wishlist route: `/wishlist`.
- Storefront predictive header search with `/api/v1/search/predictive` integration.
- Storefront product listing skeleton routes:
  - `frontend/storefront-noura/app/products/loading.jsx`
  - `frontend/storefront-noura/app/products/[id]/loading.jsx`
- New storefront documentation: `docs/frontend/storefront-enterprise-hardening.md`.

### Updated
- `GET /api/v1/admin/capabilities` now resolves capabilities via centralized RBAC policy service.
- `GET /api/v1/admin/capabilities` now allows authenticated users and resolves roles from persisted admin role assignments.
- `CustomUserDetailsService` now merges persisted `admin_user_roles` authorities and permission-derived authorities with legacy account roles.
- Admin navigation/router now include capability-gated Roles & Permissions governance route.
- Frontend role/capability fallback map expanded to enterprise role catalog and governance capability.
- Roles & permissions page now supports role create/update/deactivate, permission replacement, and user-role assignment flows.
- RBAC endpoints now enforce fine-grained permission authorities (`PERM_ROLES_*`, `PERM_USERS_*`, `PERM_AUDIT_LOGS_READ`) with admin fallback.
- Roles & permissions page now includes RBAC mutation audit-log visibility panel.
- RBAC audit logs now support date-range filtering (`occurredFrom`/`occurredTo`) and include `payloadHash` in API/UI projections.
- Roles & permissions page now supports audit-log filtering and CSV export.
- Roles & permissions page now supports permission preset selection/application and bulk user-role assignment workflows.
- Roles & permissions page bulk assignment workflow now supports actor-scoped server-persisted saved views, CSV template download, and row-level CSV import validation reporting.
- Backend and frontend READMEs updated with RBAC matrix references.
- Storefront catalog page now supports scalable URL-driven controls (`storeId`, `size`, stable filter context, pagination metadata rendering).
- Storefront API layer now normalizes relative media URLs against backend host for reliable image rendering across routes.
- Storefront cart badge refresh now listens to cart mutation events instead of route-coupled refetching.
- Storefront product cards and detail page now include persistent wishlist toggles with header badge updates.

### Fixed
- Commerce product deletions (`DELETE /api/v1/products/{productId}`) now use recovery governance `TRASH` actions (`entityType=PRODUCT`) so deleted products appear in Recovery Center and can be restored.
- Recovery Center entity filters now include `PRODUCT` to make commerce-product recovery records visible in admin UI filtering and bulk workflows.

## 2026-03-12

### Added
- Product enrichment backend endpoints under `/api/v1/products` for search, missing-field generation, per-field generation, and barcode/QR image rendering.
- Commerce persistence support for `target_audience`, `barcode`, and `qr_code` plus mirror bridge/job tables via Flyway migration `V19__product_generator_existing_products.sql`.
- Product enrichment service stack (description generation, barcode/QR utilities, mirror queueing, and scheduled mirror sync worker).
- Frontend Product Generator existing-product workflow (search, selection, missing status indicators, generate missing/per-field actions, and image rendering).
- New docs: `docs/features/product-generator.md`, `docs/features/product-sync-bridge.md`, `docs/backend-api.md`, and `docs/setup.md`.
- New docs: `docs/features/product-media-uploader.md`.
- Targeted backend tests for enrichment service behavior, mirror worker lifecycle, LLM fallback path, barcode collision handling, and security guards.

### Updated
- Product API response mapping to expose `description`, `targetAudience`, `barcode`, and `qrCode` in product detail DTOs.
- Product Generator admin API client to call new product-scoped enrichment endpoints.

### Fixed
- Product Generator warning banner class now uses existing `alert-warning` style token for correct UI rendering.
- Product media clipboard paste now accepts browser-provided image files that have no filename by normalizing them to a valid image filename before validation/upload.
- Internal product media preview URLs under `/uploads/**` are now publicly readable so browser image tags can load stored assets without auth headers.

### Refactored
- Product enrichment flow centralized in a dedicated `ProductEnrichmentService` to keep controller logic thin and reusable.
