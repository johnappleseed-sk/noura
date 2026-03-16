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
6. keep the remaining domains inside `commerce-core`

## What Stays Modular First
- pricing
- cart
- checkout
- order
- payment orchestration
- fulfillment
- customer profile
- notification
- merchant/store management

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
