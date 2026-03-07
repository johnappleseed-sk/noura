# Changelog

## 1.2.2 - 2026-03-05

- Frontend admin analytics expansion:
  - Added a new **Advanced Analytics & Reporting** module at `/analytics`.
  - Introduced multi-tab analytics workspace with broad chart coverage:
    - Revenue and order trends
    - Status and fulfillment mix
    - Weekday/hourly demand patterns
    - Product/category analytics
    - Store performance and approval workflow analytics
  - Added report tables with CSV export for:
    - top-product revenue
    - store performance
    - category health
  - Added analytics filters:
    - timeframe (`30d`, `90d`, `365d`, `all`)
    - store scope
  - Added dataset coverage notice when order pages exceed dashboard load cap.
  - Added sidebar navigation entry and route metadata for analytics.

## 1.2.1 - 2026-03-05

- Backend category-governance execution:
  - `approve category change request` now executes the requested taxonomy mutation from payload:
    - `CREATE`, `UPDATE`, `RENAME`, `MOVE`, `DELETE`
  - Added strict payload validation for approval actions (required fields, UUID parsing, supported fields).
  - Added safe-delete guards for categories:
    - blocks delete when child categories exist
    - blocks delete when products are linked to the category
  - Approval flow now updates taxonomy version only when a mutable category action is applied.
- Repository enhancements:
  - Added `CategoryRepository.existsByParentId(...)`.
  - Added `ProductRepository.existsByCategoryId(...)`.
- Test coverage:
  - Expanded `CatalogManagementServiceImplTest` with governance action tests:
    - approve/create
    - approve/rename
    - delete rejection with existing children

## 1.2.0 - 2026-03-05

- Backend enterprise catalog foundation:
  - Added hierarchical categories with tree retrieval.
  - Added attribute and attribute-set management.
  - Enhanced product model with JSONB attributes, SEO patching, and soft delete behavior.
  - Added variant list/update APIs and richer variant/media fields.
- Backend inventory foundation:
  - Added warehouses, inventory levels, reservation lifecycle, and inventory transactions.
  - Added optimistic locking on inventory rows.
  - Added backorder-aware reservation and availability checks.
- Backend pricing/promotion foundation:
  - Added price lists, price upsert API, promotion model, and active-promotion querying.
  - Added variant price quote endpoint with scope-aware resolution.
- Data model and migration updates:
  - Added `V7__catalog_inventory_pricing_foundation.sql`.
  - Added `V8__product_allow_backorder.sql`.
- Test coverage:
  - Added `InventoryServiceImplTest`.
  - Added `PricingCatalogServiceImplTest`.
- Documentation:
  - Expanded architecture and API docs with enterprise category-management model, governance, channel mapping, localization, and analytics guidance.

## 1.1.1 - 2026-03-04

- Backend checkout hardening:
  - Added optional checkout `idempotencyKey` support.
  - Added DB migration `V5__checkout_idempotency.sql` for `orders.idempotency_key` and unique `(user_id, idempotency_key)` index.
  - Added atomic inventory decrement in checkout to reduce stock-race oversell risk.
- Backend security and ownership test coverage:
  - Added method-security tests for admin-only services:
    - `AdminDashboardService.summary`
    - `OrderService.adminOrders`
    - `NotificationService.pushToUser`
    - `ProductService.createProduct`
  - Added controller security integration tests for `GET /api/v1/orders`:
    - anonymous request rejected
    - non-admin authenticated request rejected
    - JWT with invalid signature rejected
  - Added controller security integration tests for `POST /api/v1/products`:
    - anonymous request rejected
    - non-admin authenticated request rejected
    - JWT with invalid signature rejected
  - Added order ownership test for `quickReorder` to reject foreign order access.
  - Added notification ownership test for `markAsRead` to reject foreign notification access.
  - Added JWT invalid-signature/tamper rejection tests for `JwtTokenProvider`.
  - Added explicit `ACCESS_DENIED` API handling in `GlobalExceptionHandler` to return `403` instead of falling through to `500`.
  - Added explicit security exception handling in `SecurityConfig`:
    - unauthenticated/invalid-token access now returns `401` with `AUTH_REQUIRED`
    - authenticated-but-forbidden access returns `403` with `ACCESS_DENIED`
  - Added baseline security headers in `SecurityConfig`:
    - `Strict-Transport-Security`
    - `X-Content-Type-Options`
    - `X-Frame-Options`
    - `Content-Security-Policy`
    - `Referrer-Policy`
  - Added `RequestCorrelationFilter` for per-request traceability:
    - accepts optional `X-Correlation-ID`
    - generates UUID when header is missing/invalid
    - echoes `X-Correlation-ID` in responses
    - stores correlation id in MDC key `correlationId`
  - Added structured JSON logging via `logback-spring.xml`:
    - single-line JSON logs for console output
    - includes `correlationId` from MDC for request tracing
  - Hardened rate limiting forwarded-IP handling:
    - forwarded headers are honored only when `remoteAddr` matches configured trusted proxies
    - added `RATE_LIMIT_TRUSTED_PROXY_ADDRESSES` config (default: `127.0.0.1,::1`)
  - Added `RateLimitFilter` unit tests for:
    - trusted proxy + `X-Forwarded-For`
    - untrusted proxy fallback to `remoteAddr`
    - trust-disabled fallback to `remoteAddr`
    - trusted proxy + `X-Real-IP` fallback
  - Pricing/coupon integrity hardening:
    - added coupon validity window fields (`valid_from`, `valid_until`) via `V6__coupon_validity_window.sql`
    - shared `PricingService` now rejects expired and not-yet-active coupons
    - shared `PricingService` now rejects multi-coupon input in a single request
  - Added pricing tests for:
    - expired coupon rejection
    - future (not-yet-active) coupon rejection
    - multi-coupon input rejection
  - Added checkout test to ensure coupon-expiry rejection propagates before order creation.
  - Added cart ownership unit tests to reject foreign-user access for:
    - `CartServiceImpl.updateItem`
    - `CartServiceImpl.removeItem`
  - Strengthened JWT startup validation in non-local profiles to also reject:
    - default/placeholder secret patterns (for example `change-me`, `default`, `replace-me`)
    - trivial repeated-character secrets
  - Added JWT startup validator tests for default-like and repeated-character secret rejection.
  - Moved JPA auditing activation into `AuditingConfig` to keep web-slice security tests isolated from JPA auditing infrastructure.
- Frontend test reliability:
  - Fixed Jest TypeScript alias resolution by pointing `@/*` to `src/enterprise/*` in `tsconfig.jest.json`.
  - `npm test -- --runInBand` now passes for enterprise test suites.

## 1.1.0 - 2026-03-03

- Bumped backend and frontend project versions to `1.1.0`.
- Added cart clear endpoint: `DELETE /api/v1/cart/items`.
- Added notification read-management endpoints:
  - `GET /api/v1/notifications/me/unread-count`
  - `PATCH /api/v1/notifications/{notificationId}/read`
  - `PATCH /api/v1/notifications/me/read-all`
- Hardened cart item removal with ownership validation.
- Improved cart add-item stock validation to enforce cumulative quantity checks.
- Added order timeline support:
  - New table `order_timeline_events`
  - New endpoint `GET /api/v1/orders/{orderId}/timeline`
  - Automatic timeline events at checkout and status transitions.
- Added order status-change notifications for customer and admins.
- Frontend admin:
  - Added notification center with unread badge, mark-one-read, mark-all-read.
  - Added order timeline drawer in Orders management.
- Frontend enterprise:
  - Added clear-cart server sync action.
  - Added notification API integration for unread count/read actions.
