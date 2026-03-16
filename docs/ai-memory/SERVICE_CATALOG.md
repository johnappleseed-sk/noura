# Service Catalog

## Phase 1 Services

### edge-gateway
Purpose:
- single ingress
- routing
- auth enforcement
- compatibility layer for migration

### iam-platform
Purpose:
- identity and authentication

Implementation:
- Keycloak, not a custom auth service

### catalog-governance
Purpose:
- master catalog
- taxonomy
- attributes
- submissions
- review and approval
- deduplication
- catalog references

### inventory-availability
Purpose:
- stock truth
- availability
- reservation-ready inventory model

### search-discovery
Purpose:
- search index
- faceting
- browse and discovery projections

### commerce-core
Purpose:
- modular monolith for domains not worth splitting yet

Phase 1 modules inside:
- admin-governance
- merchant-network-lite
- customer-profile
- pricing-promotion
- cart
- checkout
- order-management
- payment-orchestration
- fulfillment-returns
- notification
- b2b
- marketplace

## Later Services
Split later only when justified:
- merchant-network
- assortment-offer
- payment
- customer-profile
- cart
- checkout-orchestrator
- order-management
- fulfillment-returns
- reviews-reputation
- analytics-reporting

## Service Boundary Rule
Do not create a separate service unless it has:
- clear ownership
- meaningful independent scaling or release pressure
- low enough migration risk
