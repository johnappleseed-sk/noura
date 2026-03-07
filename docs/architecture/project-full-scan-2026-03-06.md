# Project Scan — POS System → Fullstack Commerce Foundation

Date: 2026-03-06  
Scope: `c:\Users\Admin\Desktop\pos_system`

## 1) Current State Summary

This repository is a POS-first system with a strong enterprise-style backend and a **separate** customer storefront scaffold:

- Backend: Spring Boot + Spring MVC + Spring Security + JPA + Thymeleaf + HTMX
- Admin/API UI: React SPA in `admin-react/` (internal console)
- Customer storefront: `storefront-next/` (Next.js app with catalog browsing)
- Legacy transaction model: POS cash register workflows (`Sale`, POS terminal actions, stock movement history)

What already exists and can be reused directly:
- Product/catalog entities (`Product`, `Category`, `ProductVariant`, `ProductUnit`, ...).
- Inventory stock movement system (`Product`, `StockMovement`, `StockMovementService`, `ApiInventoryService`).
- Customer account skeleton (newer code): `CustomerAccount`, `CustomerAddress`, `Order`, `OrderItem`.
- JWT auth infrastructure for staff (`JwtTokenService`, `JwtAuthenticationFilter`, `AuthService`).
- Frontend scaffold pages already reading storefront catalog APIs.

## 2) Architecture map

### Backend packages of interest
- Legacy:
  - `src/main/java/com/devcore/pos_system/controller/*` (Thymeleaf/admin endpoints)
  - `src/main/java/com/devcore/pos_system/api/v1/*` (REST APIs)
  - `src/main/java/com/devcore/pos_system/service/*`
  - `src/main/java/com/devcore/pos_system/repository/*`
  - `src/main/java/com/devcore/pos_system/entity/*`
- Module-first migration folders (already started):
  - `src/main/java/com/devcore/pos_system/modules/catalog/*`
  - `src/main/java/com/devcore/pos_system/modules/customers/*`
  - `src/main/java/com/devcore/pos_system/modules/cart/*`
  - `src/main/java/com/devcore/pos_system/modules/checkout/*`
  - `src/main/java/com/devcore/pos_system/modules/orders/*`
  - `src/main/java/com/devcore/pos_system/modules/payments/*`
  - `src/main/java/com/devcore/pos_system/modules/fulfillment/*`

### Frontend
- Admin: `admin-react/`
- Customer storefront: `storefront-next/`
- Templates/assets: `src/main/resources/templates`, `src/main/resources/static`

### DB migration status
- `src/main/resources/db/migration/V1__baseline_existing_schema.sql`
- `src/main/resources/db/migration/V2__commerce_foundation.sql`
- `V2` already introduced `customer_account`, `customer_address`, `customer_order`, `customer_order_item`.

## 3) What is implemented vs what is missing

### Implemented and usable
- Catalog API (public read):
  - `GET /api/storefront/v1/catalog/categories`
  - `GET /api/storefront/v1/catalog/products`
  - `GET /api/storefront/v1/catalog/products/{id}`
  - `GET /api/storefront/v1/catalog/products/{id}/availability`
- Frontend storefront pages:
  - home, catalog listing, product detail pages

### Present but incomplete for enterprise commerce
- Customer identity for public store (currently no dedicated storefront auth)
- Cart/checkout persistence
- Order placement + payment abstraction for web channel
- Shipment/fulfillment lifecycle
- Product/customer roles separation for staff vs customer security
- Payment, notifications, returns, marketplace, admin reporting for web orders

## 4) Key risks before full conversion

1. **Security coupling**  
   `JwtAuthenticationFilter` only resolves staff `AppUser` identities; customer tokens would not authenticate.
2. **No storefront customer workflow**  
   Only catalog browsing exists today; checkout cannot progress from cart to paid order.
3. **Schema mismatch risk if adding tables casually**  
   `Order` and `OrderItem` exist, but cart/session tables are missing.
4. **Config drift and naming mismatch already being cleaned**  
   Some docs and config settings still reflect POS-only assumptions.

## 5) Recommended phase 1 implementation plan (this run)

### Step 1 — Complete scan and reference
- Done in this doc for future handoff.

### Step 2 — Add customer storefront auth API
- New endpoints:
  - `POST /api/storefront/v1/customers/register`
  - `POST /api/storefront/v1/customers/login`
  - `GET /api/storefront/v1/customers/me`
- Keep staff auth untouched (`/api/v1/auth/**`).

### Step 3 — Add cart + order foundation
- Add `customer_cart` + `customer_cart_item` entities/repositories.
- Add:
  - `GET /api/storefront/v1/cart`
  - `POST /api/storefront/v1/cart/items`
  - `PATCH /api/storefront/v1/cart/items/{itemId}`
  - `DELETE /api/storefront/v1/cart/items/{itemId}`
  - `DELETE /api/storefront/v1/cart`
  - `POST /api/storefront/v1/orders/checkout`
  - `GET /api/storefront/v1/orders/me`
- Checkout should:
  - validate stock with existing product logic
  - call existing `StockMovementService.recordSale(...)`
  - persist order + items

## 6) Current priority for future work

1. Implement secure storefront auth channel separate from staff auth.
2. Persist cart and attach it to authenticated customer account.
3. Build checkout service with inventory-safe stock reservation/consume + order creation.
4. Add storefront cart/checkout UI in `storefront-next`.
5. Add tests for catalog-read + customer auth + cart mutation + checkout.
