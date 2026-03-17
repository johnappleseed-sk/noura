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

### payment-service
Purpose:
- internal payment intent records
- provider abstraction boundary
- authorize/capture orchestration
- webhook delivery deduplication

Notes:
- extracted ahead of broader commerce-core breakup because provider credentials, webhook handling, and payment state isolation justify an explicit boundary
- reads immutable order totals from `order-service`
- does not mutate order state in the current slice

### promotion-service
Purpose:
- promotion definitions and admin CRUD
- promo-code and coupon validation
- deterministic cart discount evaluation
- promotion scope and eligibility ownership

Notes:
- extracted because promotions already acted like a separate operating surface in admin/storefront flows
- reuses archived deterministic rule evaluation instead of introducing a generic offer engine
- does not own base product prices; those remain in `pricing-service`

### shipping-service
Purpose:
- shipping method discovery
- rule-based shipping quote calculation
- shipment lifecycle ownership
- carrier abstraction boundary
- fulfillment-status hook foundation

Notes:
- extracted ahead of broader fulfillment decomposition because shipment state, tracking references, and later carrier callbacks form a clean boundary
- reads order ownership and shipping-address snapshots from `order-service`
- does not mutate order state in the current slice

### review-service
Purpose:
- storefront product review submission
- moderation workflow ownership
- approved-rating aggregation
- spam-ready review submission metadata

Notes:
- extracted because product feedback and moderation are a separate operational concern from catalog identity ownership
- reuses the archived `ProductReview` flow but keeps the first slice deterministic and product-scoped
- validates product identity through read-only `catalog-service` lookups
- does not back-write rating aggregates into catalog/product records in the current slice

### commerce-core
Purpose:
- modular monolith for domains not worth splitting yet

Phase 1 modules inside:
- admin-governance
- merchant-network-lite
- customer-profile
- pricing
- cart
- checkout
- order-management
- fulfillment-returns
- notification
- b2b
- marketplace

## Later Services
Split later only when justified:
- merchant-network
- assortment-offer
- customer-profile
- cart
- checkout-orchestrator
- order-management
- fulfillment-returns
- analytics-reporting

## Service Boundary Rule
Do not create a separate service unless it has:
- clear ownership
- meaningful independent scaling or release pressure
- low enough migration risk
