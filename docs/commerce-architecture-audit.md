# Commerce Architecture Audit Report

## Scope reviewed

- Backend: pricing/promotions, inventory, cart, checkout, orders, carousel/content, analytics exposure
- Storefront: home promotions, product detail, cart, checkout start/completion flow, API usage
- Admin dashboard: pricing, analytics, inventory management surfaces

## Key weaknesses discovered

### 1. Domain duplication across backend trees

The codebase currently mixes multiple commerce implementations:

- root platform modules under `com.noura.platform.*`
- a separate `commerce.*` tree
- a separate `/api/inventory/v1` inventory subsystem

This creates overlapping service boundaries for inventory, pricing, and order behavior.

### 2. Promotions were split across two mechanisms

Before this change:

- `Coupon` drove checkout/cart totals
- `Promotion` only affected active-promotion listing and variant quote logic

That meant admin promotion configuration did not fully map to storefront/cart behavior.

### 3. Analytics events were largely missing

The platform had category analytics, but not a reusable event ingestion pipeline for:

- product views
- add/remove from cart
- checkout started/completed
- promotion application

### 4. Inventory operations lacked enterprise coordination

The root inventory module supported:

- stock levels
- reservations
- adjustments

But it lacked enterprise operational models for:

- warehouse transfers
- scheduled restocks
- consolidated low-stock alerts
- reservation operations visibility for admin

### 5. Admin module coverage was uneven

Existing admin pages exposed pricing, analytics, and inventory in isolated ways, but not as enterprise operating surfaces with:

- rule management
- event observability
- inventory workflow controls

## Architectural decisions implemented

### Promotions

- Kept `Promotion` as the canonical rule entity instead of creating a parallel discount table.
- Extended the existing pricing boundary with richer promotion fields and rule evaluation semantics.
- Added a dedicated admin promotion service/controller for enterprise operations.

### Analytics

- Added a single event ingestion module for commerce behavior.
- Captured authoritative backend events from cart and checkout services.
- Added lightweight storefront-origin product view and checkout-start signals.

### Inventory

- Added additive operational entities (`inventory_transfers`, `inventory_restock_schedules`) to the root inventory model.
- Left the existing inventory subsystem intact, but exposed the split as an architectural concern for future consolidation.

## New modules implemented

### Promotions engine

- Richer promotion entity fields: `code`, `description`, `stackable`, usage limits, customer segment, archive state
- New promotion types:
  - `CART_THRESHOLD_DISCOUNT`
  - `PRODUCT_BUNDLE_DISCOUNT`
- Admin APIs:
  - `GET /api/v1/admin/promotions`
  - `GET /api/v1/admin/promotions/{id}`
  - `PATCH /api/v1/admin/promotions/{id}`
  - `POST /api/v1/admin/promotions/evaluate`
- Existing public API retained:
  - `GET /api/v1/promotions/active`
- Promotion rule engine now evaluates cart/subtotal/item-based rules and free shipping flags.

### Analytics event system

- Added `analytics_events` persistence model and event ingestion API
- New APIs:
  - `POST /api/v1/analytics/events`
  - `GET /api/v1/admin/analytics/overview`
- Tracks:
  - `PRODUCT_VIEW`
  - `ADD_TO_CART`
  - `REMOVE_FROM_CART`
  - `CHECKOUT_STARTED`
  - `CHECKOUT_COMPLETED`
  - `PROMOTION_APPLIED`
- Used by:
  - admin analytics dashboard panel
  - storefront product detail tracking
  - backend cart/checkout services

### Advanced inventory foundations

- Added operational entities:
  - `inventory_transfers`
  - `inventory_restock_schedules`
- New admin APIs:
  - `GET /api/v1/admin/inventory/alerts/low-stock`
  - `GET /api/v1/admin/inventory/reservations`
  - `GET /api/v1/admin/inventory/transfers`
  - `POST /api/v1/admin/inventory/transfers`
  - `GET /api/v1/admin/inventory/restock-schedules`
  - `POST /api/v1/admin/inventory/restock-schedules`

## Storefront integration notes

- Product detail page now emits `PRODUCT_VIEW` events.
- Cart page emits `CHECKOUT_STARTED` before checkout submission.
- Backend cart/checkout APIs emit:
  - add/remove from cart
  - promotion applied
  - checkout completed
- Promotion DTOs now expose `code`, `description`, `discountPercent`, and `discountAmount`, which aligns better with storefront home/deals rendering.

## Admin integration notes

- Pricing page now hosts a dedicated enterprise promotion panel.
- Analytics page now hosts a dedicated commerce event overview panel.
- Inventory page now hosts a dedicated enterprise inventory operations panel.

## Validation and business rule notes

- Promotion windows reject `endDate < startDate`
- Promotion codes are unique when present
- Non-stackable promotions short-circuit subsequent promo application after a successful match
- Transfer source and destination warehouses must differ
- Immediate transfers require sufficient available stock
- Analytics ingestion is non-blocking from the storefront and should never break commerce UX

## Permission notes

- Public:
  - `POST /api/v1/analytics/events`
  - `GET /api/v1/promotions/active`
- Admin:
  - promotion admin endpoints
  - analytics overview endpoint
  - enterprise inventory endpoints

## Migration notes

Migration added:

- new promotion columns and indexes
- `analytics_events`
- `inventory_transfers`
- `inventory_restock_schedules`

## Known assumptions

- The root platform inventory model remains the canonical target for the new transfer/restock APIs.
- The separate `/api/inventory/v1` subsystem is not yet consolidated into the same model.
- Per-customer promotion usage counting is modeled but not fully enforced without a unified customer-order-promotion ledger.

## Recommended next steps

1. Consolidate the split inventory stacks into one canonical warehouse/stock model.
2. Add persistent promotion redemption history for per-customer and total usage enforcement.
3. Add merchandising and recommendation services on top of the analytics event stream.
4. Add search indexing and ranking hooks to unify catalog, promotions, and availability.
5. Add approval workflow and version history for promotions/content changes.
