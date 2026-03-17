# Migration Plan

## Migration Goal
Move from the current hybrid backend into a governed platform without a big-bang rewrite.

## Current State
Current repo contains:
- a Spring Boot backend with mixed modular and legacy structure
- an admin app
- a storefront app
- existing RBAC and catalog/inventory foundations

## Phase 1 Objective
Build the practical startup foundation for the future enterprise platform.

## Phase 1 First Slice
Implement the supply-control loop:

`merchant/store setup -> product submission -> dedupe/review -> master catalog approval -> catalog reference -> inventory availability -> search visibility`

## Extraction Order
1. stabilize repo and architecture memory
2. introduce gateway, IAM, CI/CD, observability
3. build `catalog-governance`
4. extract `inventory-availability`
5. build `search-discovery`
6. extract `promotion-service` to isolate discount rule ownership and promo-code evaluation
7. extract `payment-service` to isolate provider lifecycle and webhook handling
8. extract `shipping-service` to isolate shipment lifecycle and future carrier callbacks
9. keep the remaining domains inside `commerce-core`

## What Stays Modular First
- pricing
- cart
- checkout
- order
- fulfillment
- customer profile
- notification
- merchant/store management

Promotion extraction note:
- promotion moved out before the rest of commerce-core because admin/storefront discount workflows already behaved like a separate bounded context and deterministic archived logic was worth reusing directly

Payment extraction note:
- payment moved out earlier than the other transaction modules because provider integrations and webhook retries create a cleaner operational boundary when isolated

Shipping extraction note:
- shipping moved out before the broader fulfillment breakup because tracking identifiers, carrier polling/callbacks, and shipment status transitions are a cleaner operational boundary when isolated

## Data Migration Rules
- one PostgreSQL cluster first, separate schemas and DB users
- no cross-service writes
- use transactional outbox for integration
- keep search as projection only

## API Migration Rules
- gateway is the stable public entry point
- preserve client compatibility while internals move
- publish OpenAPI and AsyncAPI contracts for new boundaries

## Operational Migration Rules
- feature flags for risky changes
- canary where possible
- rollback by route switch before DB rollback
- every extracted service needs dashboards and runbooks

## Success Criteria
- approved products create canonical catalog records
- merchant/store references do not create duplicates
- inventory availability is visible by canonical SKU
- search is fed from events and stays fresh enough for pilot operations
