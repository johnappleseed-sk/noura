## 2026-03-18

### Added
- Extracted `search-service` application layer for projection-backed product search, predictive suggestions, trend tags, and internal indexing operations.
- New search-provider abstraction (`ProductSearchIndexProvider`) adapted from archived search adapter patterns so runtime storage can move from PostgreSQL to OpenSearch later.
- New PostgreSQL search projection foundation migration: `services/search-service/src/main/resources/db/migration/V1__search_projection_foundation.sql`.
- New search-service endpoints:
  - `GET /api/v1/search/products`
  - `GET /api/v1/search/predictive`
  - `GET /api/v1/search/trend-tags`
- New internal search indexing endpoints:
  - `POST /internal/search/index/products`
  - `POST /internal/search/index/products/rebuild`
  - `DELETE /internal/search/index/products/{productId}`
- New search-service tests for public query APIs, indexing APIs, provider behavior, and projection rebuild flow.
- New search documentation:
  - `docs/api/search-service.md`
  - `docs/architecture/search-service.md`

### Updated
- Backend/frontend contract alignment for active storefront and admin flows:
  - `catalog-service` now exposes storefront compatibility routes for `/api/v1/merchandising/products`, `/api/v1/recommendations/**`, `/api/v1/recommendations/mock-ai`, `/api/v1/products/{productId}/related`, and `/api/v1/products/{productId}/frequently-bought-together`
  - `catalog-service` now exposes admin compatibility routes for `/api/v1/admin/recommendations/**` and `/api/v1/admin/merchandising/**`
  - `shipping-service` now exposes extracted merchant/store/service-area compatibility routes for `/api/v1/admin/merchants/**`, `/api/v1/admin/stores/**`, `/api/v1/stores/**`, and `/api/v1/admin/service-areas/**`
  - `checkout-service` now supports legacy storefront checkout step routes under `/api/v1/checkout/steps/**`
  - `customer-service` now supports `/api/v1/account/payment-methods/**`
  - `order-service` now supports `POST /api/v1/account/orders/{orderId}/quick-reorder`
  - `pricing-service` now supports `/api/v1/price-lists`, `/api/v1/prices`, and `/api/v1/prices/variants/{variantId}`
- `apps/api-gateway` now routes the new catalog and shipping compatibility contracts to extracted services instead of falling through to the legacy `app-service`.
- `apps/admin-web` and `apps/storefront-web` both build successfully against the updated contract surface; storefront build still logs expected `ECONNREFUSED` warnings when local APIs are absent during prerender.
- `checkout-service` now executes the full synchronous purchase path:
  - validates cart/address/pricing/inventory
  - reserves stock
  - creates the order in `PAYMENT_PENDING`
  - creates and confirms payment intents through `payment-service`
  - finalizes the order to `PAID`
  - clears the cart and dispatches notifications best-effort
- `order-service` now exposes `POST /internal/orders/{orderId}/status` for trusted internal order finalization.
- `customer-service` internal lookup is now used by checkout so notification dispatch targets the real customer UUID instead of the external subject.
- `apps/storefront-web` checkout now sends `storeId`, typed payment fields, and generated idempotency keys to the backend direct-checkout contract.
- `apps/storefront-web` order normalization now understands checkout responses that return nested `order` and `payment` summaries.
- `services/search-service/README.md` with the search boundary decision, indexing model, query-ownership rules, and environment-variable guidance.
- Root `README.md`, `services/README.md`, and `docs/backend-api.md` to reflect the extracted `search-service`.
- `apps/admin-web` control-center endpoint catalog now exposes `GET /api/v1/search/products` and uses the correct predictive-search query parameters.
- Search boundary guidance now explicitly documents that `catalog-service` keeps product truth plus browse/admin product search while `search-service` owns `/api/v1/search/**`.
- Search-service public DTOs now include storefront-compatibility aliases after frontend audit:
  - `/api/v1/search/products` also accepts `query`
  - product hits expose `id`, `isTrending`, and `merchandisingScore` aliases
  - trend tags expose `name` and `tag` aliases in addition to `value`
- `apps/api-gateway` now routes payment and shipping traffic (plus health/readiness probes) through:
  - `/api/v1/payments/**` and `/api/payments/**`
  - `/api/v1/shipping/**` and `/api/shipping/**`
  - `/internal/payment-service/{health|readiness}`
  - `/internal/shipping-service/{health|readiness}`
- `cart-service` now supports coupon lifecycle endpoints:
  - `POST /api/v1/cart/coupon`
  - `DELETE /api/v1/cart/coupon`
- `cart-service` now validates coupons through `promotion-service` and persists deterministic `discountAmount`/`totalAmount` updates in cart totals.
- `notification-service` API envelope now aligns with the shared cross-service contract by including `correlationId` and field-level validation errors.
- `notification-service` now propagates `X-Correlation-ID` with MDC-backed request filtering.
- `promotion-service` monetary normalization now uses scale `4` to align with cart/order/checkout/pricing/payment/shipping totals.

## 2026-03-17

### Added
- Extracted `review-service` application layer for storefront review submission, product review reads, approved-rating aggregation, and admin moderation.
- New review moderation model with `PENDING`, `APPROVED`, and `REJECTED` workflow states plus spam-ready submission metadata fields.
- New review service Flyway foundation migration: `services/review-service/src/main/resources/db/migration/V1__review_foundation.sql`.
- New review-service endpoints:
  - `GET /api/v1/products/{productId}/reviews`
  - `POST /api/v1/products/{productId}/reviews`
  - `GET /api/v1/products/{productId}/rating-summary`
  - `POST /api/v1/admin/reviews/{reviewId}/approve`
  - `POST /api/v1/admin/reviews/{reviewId}/reject`
- New review API tests for request-context resolution, duplicate-review prevention, moderation authorization, pending-first submission, and approved-only rating aggregation.
- New review documentation:
  - `docs/api/review-service.md`
  - `docs/architecture/review-service.md`
- Extracted `promotion-service` application layer for promotion CRUD, promo-code validation, and deterministic cart discount evaluation.
- New promotion persistence model and migration: `services/promotion-service/src/main/resources/db/migration/V1__promotion_foundation.sql`.
- New promotion admin and storefront-compatible endpoints:
  - `GET /api/v1/promotions/active`
  - `POST /api/v1/promotions/validate-code`
  - `POST /api/v1/promotions/evaluate`
  - `POST /api/v1/promotions`
  - `GET /api/v1/admin/promotions`
  - `GET /api/v1/admin/promotions/{promotionId}`
  - `PATCH /api/v1/admin/promotions/{promotionId}`
  - `DELETE /api/v1/admin/promotions/{promotionId}`
  - `POST /api/v1/admin/promotions/evaluate`
- New promotion API tests for request-context resolution, authorized admin mutation, deterministic evaluation ordering, validation failure reasons, and identifier-space collision checks.
- New promotion documentation:
  - `docs/api/promotion-service.md`
  - `docs/architecture/promotion-service.md`
- Extracted `shipping-service` application layer for shipping method discovery, rule-based quotes, shipment creation, shipment reads, and internal fulfillment-status hooks.
- New pluggable shipping carrier boundary with deterministic internal rule-based carrier adapter for startup-safe shipping integration.
- New shipment lifecycle model with derived fulfillment-hook mapping for order shipment status foundations.
- New shipping service Flyway foundation migration: `services/shipping-service/src/main/resources/db/migration/V1__shipping_foundation.sql`.
- New shipping API tests for request-context resolution, rule-based carrier rules, idempotent shipment creation, and shipment status transition validation.
- New shipping documentation:
  - `docs/api/shipping-service.md`
  - `docs/architecture/shipping-service.md`
- Extracted `payment-service` application layer for internal payment intent creation, confirm flows, lookup APIs, and webhook-ready status handling.
- New pluggable payment provider boundary with deterministic sandbox/mock adapter for startup-safe integration.
- New payment lifecycle model with explicit authorization/capture sub-state tracking.
- New payment webhook delivery table and idempotent provider-event deduplication migration: `services/payment-service/src/main/resources/db/migration/V2__payment_lifecycle_and_webhooks.sql`.
- New payment API tests for request-context resolution, sandbox provider behavior, idempotent create flow, manual status transitions, and webhook processing.
- New payment documentation:
  - `docs/api/payment-service.md`
  - `docs/architecture/payment-service.md`

### Updated
- `services/review-service/README.md` with moderation rules, endpoint coverage, spam-ready field notes, and local-run guidance.
- `services/README.md`, root `README.md`, and `docs/backend-api.md` to reflect `review-service` extraction.
- `apps/api-gateway` now routes review-service API and health/readiness traffic via:
  - `/api/v1/products/{productId}/reviews`
  - `/api/v1/products/{productId}/rating-summary`
  - `/api/v1/admin/reviews/**`
- `apps/admin-web` control-center endpoint catalog now exposes review-service storefront and moderation endpoints explicitly.
- `services/promotion-service/README.md` with endpoint coverage, deterministic evaluation rules, identifier constraints, and local-run notes.
- `services/README.md`, root `README.md`, and `docs/backend-api.md` to reflect `promotion-service` extraction.
- `apps/api-gateway` now routes promotion-service API and health/readiness traffic via:
  - `/api/v1/promotions/**`
  - `/api/v1/admin/promotions/**`
- `services/shipping-service/README.md` with quote rules, shipment lifecycle, scenario metadata, and environment-variable guidance.
- `services/README.md`, root `README.md`, and `docs/backend-api.md` to reflect `shipping-service` extraction.
- Shipping service application config now supports:
  - `ORDER_SERVICE_INTERNAL_API_KEY`
  - `SHIPPING_RULE_BASED_CARRIER_CODE`
  - `SHIPPING_RULE_BASED_DISPLAY_NAME`
  - `SHIPPING_RULE_BASED_FREE_STANDARD_THRESHOLD`
  - `SHIPPING_RULE_BASED_SAME_DAY_COUNTRY_CODE`
  - `SHIPPING_RULE_BASED_SAME_DAY_CITIES`
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
