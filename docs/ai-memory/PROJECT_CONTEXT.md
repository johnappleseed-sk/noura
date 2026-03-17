# Project Context

## What We Are Building
Noura is a startup commerce platform being re-initialized into an enterprise-ready architecture.

This is **not** a greenfield rewrite.
This is a controlled migration from the current codebase into a scalable platform.

## Platform Scope
The platform must support:
- B2B
- B2C
- C2C marketplace
- multi-vendor merchant stores
- partner contract stores
- branch stores
- merchant-managed stores

## Strategic Core
The platform is built around:
- centralized product governance
- a master catalog
- super inventory without duplicated global products
- strong operational control
- future regional expansion

## Current Delivery Posture
We already completed:
- architecture strategy
- migration strategy
- service decomposition
- super inventory and master catalog design
- business strategy
- Phase 1 implementation planning

## Current Codebase Reality
The repository already contains:
- a Spring Boot backend
- a React admin dashboard
- a Next.js storefront
- extracted `order-service`, `checkout-service`, `search-service`, `promotion-service`, `payment-service`, `shipping-service`, and `review-service` targets
- docs and partial domain foundations

The current backend is a hybrid:
- partly modular by domain
- partly legacy shared-layer structure

## Near-Term Goal
Phase 1 creates the startup-safe platform foundation:
- gateway
- IAM
- catalog-governance
- inventory-availability
- search-discovery
- extracted `search-service` for projection-backed discovery queries and search indexing abstraction
- extracted `promotion-service` for deterministic discount rule ownership and promo-code evaluation
- extracted `payment-service` for provider abstraction and webhook ownership
- extracted `shipping-service` for shipment lifecycle ownership and carrier abstraction
- extracted `review-service` for moderated customer feedback ownership and approved-rating aggregation
- modular `commerce-core`

## Non-Negotiables
- no careless full rewrite
- no duplicate global product masters
- no search index as source of truth
- no direct cross-service database writes
- no premature microservice fragmentation
- keep architecture and business strategy aligned

## Read Order
For architecture or implementation work, read:
1. `PROJECT_CONTEXT.md`
2. `RECENT_CHANGES.md`
3. `BUSINESS_MODEL.md`
4. `ARCHITECTURE.md`
5. `DOMAIN_MAP.md`
6. `SERVICE_CATALOG.md`
7. `MIGRATION_PLAN.md`
8. `ENGINEERING_PRINCIPLES.md`
9. `AI_COLLABORATION_RULES.md`
