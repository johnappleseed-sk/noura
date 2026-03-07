# POS to Enterprise E-Commerce Roadmap

Status: initial scan  
Date: 2026-03-06

## Purpose

This document records the current state of the repository and a pragmatic path to evolve it from an internal POS/back-office system into a fullstack enterprise e-commerce platform.

It is intended to be the working reference for future implementation turns.

## Current System Snapshot

### What the project is today

The current system is an enterprise-style retail operations platform with strong POS and back-office coverage:

- Spring Boot MVC backend with Spring Security and Spring Data JPA.
- Thymeleaf + HTMX server-rendered POS and admin pages.
- Separate React admin frontend in `admin-react/`.
- MySQL-oriented runtime configuration with H2 tests.
- Optional local device bridge in `bridge/pos-bridge-node/`.

### Core business capabilities already implemented

The repository already has strong reusable retail foundations:

- Catalog and category management.
- Dynamic attributes, variants, exclusions, and unit conversions.
- Tier pricing and customer-group pricing.
- Inventory movements, stock adjustments, receiving, and purchase orders.
- POS cart, checkout, hold/resume, receipts, receipt PDF, and hardware hooks.
- Shift management and terminal settings.
- Supplier management.
- Sales history, returns, voids, and reporting.
- JWT + OTP auth API, MVC login flow, SSO support, and role/permission controls.
- Audit logging.

### Main code surfaces

- MVC controllers: `src/main/java/com/devcore/pos_system/controller/`
- REST API controllers: `src/main/java/com/devcore/pos_system/api/v1/controller/`
- Legacy horizontal services: `src/main/java/com/devcore/pos_system/service/`
- Legacy horizontal entities: `src/main/java/com/devcore/pos_system/entity/`
- Legacy horizontal repositories: `src/main/java/com/devcore/pos_system/repository/`
- Module-first migration start: `src/main/java/com/devcore/pos_system/modules/currency/`
- Server-rendered pages: `src/main/resources/templates/`
- Separate admin SPA: `admin-react/src/`

### Notable confirmed assets we can reuse

- `Product`, `Category`, `ProductVariant`, `ProductUnit`, `SkuSellUnit`, `SkuUnitTierPrice`
- `StockMovement`, `SkuInventoryBalance`, `GoodsReceipt`, `PurchaseOrder`
- `MarketingCampaign`, `PricingService`, `MarketingPricingService`
- `Customer`, `CustomerGroup`
- `AuthService`, `JwtTokenService`, `SecurityConfig`
- `AuditEventService`
- `ApiProductService`, `ApiInventoryService`, `ApiReportsService`, `ApiSupplierService`, `ApiUserService`

## What This Project Is Not Yet

This is not yet an e-commerce platform. It is still a retail operations system.

The clearest signals:

- `Sale` is modeled as a store transaction with `cashierUsername`, `terminalId`, and `shift_id`, which makes it POS-oriented rather than a customer web order.
- `Customer` is currently a lightweight business record with `name`, `phone`, `email`, `points`, `wholesale`, and `customerGroup`; it is not a full customer account model.
- There is no online order aggregate, shipment aggregate, fulfillment workflow, customer address book, return authorization flow, or payment gateway integration.
- The React frontend is an internal admin app only. It has pages for dashboard, users, products, inventory, suppliers, reports, and audit, but no storefront.

## Current Architecture Risks and Cleanup Items

These should be fixed before large-scale commerce work.

### 1. Package and project identity mismatch

- `pom.xml` still uses `groupId` `com.example`.
- The application code lives under `com.devcore.pos_system`.

This will create avoidable friction for packaging, documentation, artifact identity, and future module extraction.

### 2. Configuration drift in `application.properties`

`src/main/resources/application.properties` currently mixes baseline and dev-style settings:

- duplicate keys exist
- verbose error settings are enabled at the bottom of the file
- `spring.jpa.hibernate.ddl-auto=update` is still enabled

That is acceptable for rapid development, but not for enterprise-grade schema control.

### 3. Flyway is configured but repository migrations are missing

The application config points Flyway at `classpath:db/migration`, but `src/main/resources/db/migration` is currently absent from the repository.

Current schema evolution appears to rely on:

- `spring.jpa.hibernate.ddl-auto=update`
- manual SQL files under `src/main/resources/sql/`

That is too weak for a major domain expansion like e-commerce.

### 4. Module migration is only partially done

The repository has an explicit module-first target structure in `docs/architecture/backend-frontend-split-proposal.md`, but only `modules/currency` has actually moved.

Most domain logic still lives in horizontal packages:

- `controller`
- `service`
- `repository`
- `entity`

### 5. Template duplication is already present

The repository contains duplicated page structures under both:

- `src/main/resources/templates/*`
- `src/main/resources/templates/pages/*`

That is survivable short-term, but it will become expensive once a storefront layer is added.

### 6. Role taxonomy is inconsistent

`UserRole` currently mixes two naming schemes:

- `SUPER_ADMIN`, `BRANCH_MANAGER`, `INVENTORY_STAFF`
- `ADMIN`, `MANAGER`, `CASHIER`

The React admin normalizes both sets, and security rules authorize against both concepts. This should be unified before customer-facing identity and multi-channel authorization are added.

### 7. Auth API is staff-oriented, not customer-oriented

`AuthService.register(...)` currently creates new users with role `CASHIER`.

That is useful for internal auth testing, but it is not a valid foundation for public customer sign-up.

### 8. Codebase contains generated duplicate Javadoc noise

Several classes, especially in security and service layers, include repeated generated Javadoc blocks. This increases maintenance cost and reduces signal while reading critical code.

## Reuse Map for E-Commerce

### Reuse mostly as-is

- Catalog basics: products, categories, variants, units
- Inventory and stock accounting
- Supplier and procurement functions
- Audit logging
- Reporting foundations
- Internal admin RBAC concepts
- Multi-currency and pricing primitives

### Reuse after adaptation

- `Customer`: keep as a business concept, but split into `CustomerAccount`, `CustomerProfile`, `CustomerAddress`, `CustomerPreference`, and possibly `B2BAccount`
- `Sale`: keep for POS history only; do not force it to become the public web order model
- `MarketingCampaign`: adapt into a broader promotions engine
- Auth stack: keep JWT, OTP, and security plumbing, but create a separate customer identity flow
- React admin: keep as the internal console, expand it instead of replacing it

### Build new from zero

- Storefront frontend
- Public catalog browse/search APIs
- Customer account lifecycle
- Cart and checkout session model
- Online order model
- Payment orchestration and gateway abstraction
- Shipment and fulfillment model
- Delivery options and tracking
- Returns/RMA workflow
- Notifications
- SEO/content/discoverability layer

## Recommended Target Platform

### Recommendation

Keep the backend as a modular monolith first. Do not split into microservices yet.

Recommended target shape:

- Spring Boot backend remains the system of record.
- MySQL remains the primary database.
- `admin-react` remains the internal admin application.
- Existing Thymeleaf POS stays online during the migration.
- Add a dedicated customer storefront frontend.

### Frontend recommendation

For enterprise e-commerce, the recommended storefront is a dedicated SSR-capable React app.

Recommended option:

- `storefront-next/` for customer-facing web commerce

Reason:

- SEO matters for product/catalog pages
- better control over landing pages and content rendering
- clearer separation between customer UX and internal admin UX

Fallback option:

- a Vite React storefront if the immediate priority is internal ordering, private B2B commerce, or pilot delivery without SEO requirements

## Recommended Target Domain Modules

The current long-term package direction is correct. It should be extended into commerce modules like this:

- `platform/`
- `shared/`
- `modules/auth/`
- `modules/users/`
- `modules/catalog/`
- `modules/pricing/`
- `modules/promotions/`
- `modules/inventory/`
- `modules/procurement/`
- `modules/pos/`
- `modules/customers/`
- `modules/cart/`
- `modules/checkout/`
- `modules/orders/`
- `modules/payments/`
- `modules/fulfillment/`
- `modules/notifications/`
- `modules/reporting/`
- `modules/audit/`

## Target Domain Model Additions

These are the minimum missing aggregates for e-commerce.

### Customer-facing identity

- `CustomerAccount`
- `CustomerProfile`
- `CustomerAddress`
- `CustomerSession`
- `PasswordResetToken`
- `EmailVerificationToken`

### Commerce flow

- `Cart`
- `CartItem`
- `CheckoutSession`
- `CouponRedemption`
- `Order`
- `OrderItem`
- `OrderStatus`
- `OrderPayment`
- `OrderStatusHistory`

### Fulfillment

- `Shipment`
- `ShipmentItem`
- `ShipmentStatus`
- `CarrierService`
- `TrackingEvent`
- `FulfillmentLocation`

### Enterprise extensions

- `SalesChannel`
- `StorefrontConfiguration`
- `InventoryReservation`
- `PaymentTransaction`
- `RefundTransaction`
- `ReturnRequest`
- `ReturnItem`
- `NotificationLog`

## Phase Plan

### Phase 0: Stabilize the foundation

Goal: make the repository safe for long-term commerce changes.

- align `pom.xml` identity with `com.devcore`
- clean `application.properties`
- introduce real Flyway migrations and baseline the current schema
- stop depending on `ddl-auto=update`
- continue moving horizontal packages into module-first structure
- remove or reduce template duplication
- normalize role taxonomy
- define whether customer auth is separate from staff auth

### Phase 1: Build the commerce backend core

Goal: add e-commerce domain objects without breaking POS.

- add customer account and address model
- add cart and checkout session model
- add order and order-item model
- add payment transaction abstraction
- add shipment and tracking model
- add inventory reservation logic for web checkout
- expose public read APIs for catalog, category, pricing, and availability

Important rule:

- POS `Sale` should remain a retail transaction record
- web commerce should get its own `Order` model

### Phase 2: Launch the storefront

Goal: customer-facing web commerce.

- home page
- category listing pages
- product detail pages
- search and filters
- customer registration/login/account pages
- cart
- checkout
- order history
- order detail and tracking pages

### Phase 3: Enterprise operations

Goal: make the platform operationally serious.

- payment provider integrations
- shipment carrier integrations
- notification service
- stronger observability
- admin workflows for fulfillment and returns
- fraud/risk checks
- promotional rules and coupon engine
- richer analytics

### Phase 4: Multi-channel expansion

Goal: support larger-scale retail/e-commerce convergence.

- multi-store or multi-brand support
- marketplace connectors
- B2B pricing and account hierarchies
- warehouse-level fulfillment routing
- channel-aware inventory allocation

## Suggested First Implementation Sprint

If we start converting now, the first sprint should be small and structural, not flashy.

### Sprint 1 scope

1. Fix project hygiene:
   - normalize project identity
   - clean app config
   - add Flyway baseline
2. Create new module skeletons:
   - `catalog`
   - `customers`
   - `cart`
   - `checkout`
   - `orders`
   - `payments`
   - `fulfillment`
3. Add first new entities:
   - `CustomerAccount`
   - `CustomerAddress`
   - `Order`
   - `OrderItem`
4. Add first public APIs:
   - catalog list
   - catalog detail
   - availability
5. Scaffold storefront app

### Why this sprint order is correct

Without schema discipline and a new commerce order model, every later feature will either break POS concepts or deepen technical debt.

## Concrete Mapping from POS Concepts to E-Commerce Concepts

### Keep

- `Product` -> catalog item base
- `Category` -> catalog navigation
- `ProductVariant` -> sellable SKU variant
- `ProductUnit` and `SkuSellUnit` -> sellable unit model
- `SkuInventoryBalance` -> stock source for reservations and availability
- `MarketingCampaign` -> promotion seed

### Split

- `Customer` -> operational customer record plus future customer-account domain
- `Sale` -> keep for POS only; do not overload into e-commerce order
- `AuthService` -> split staff auth from customer auth concerns

### Add beside current model

- `Order` beside `Sale`
- `Shipment` beside `GoodsReceipt`
- `CustomerAddress` beside `Supplier.address`
- `PaymentTransaction` beside current POS payment records

## Recommended Working Rules During Conversion

- Keep POS behavior stable while introducing e-commerce modules.
- Add new APIs instead of mutating existing POS routes unless there is a strong reason.
- Prefer module-first packages for all new code.
- Do not force the customer storefront into Thymeleaf.
- Keep admin and storefront UIs separate.
- Put schema changes behind Flyway before adding net-new commerce tables.
- Add tests around new commerce flows from the start.

## Immediate Next Tasks

The most sensible next implementation steps are:

1. create the project hygiene backlog and fix the config/schema baseline
2. scaffold the new commerce modules in the backend
3. define the first storefront API contract
4. scaffold the storefront frontend
5. implement customer account, cart, and order foundations

