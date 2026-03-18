# Architecture

## Target Shape
Use a phased architecture with three planes:

### Control Plane
Owns governed business truth:
- IAM
- admin governance
- merchant / store / contract management
- master catalog
- taxonomy and attributes

### Transaction Plane
Owns operational commerce flows:
- inventory
- pricing
- promotions
- cart
- checkout
- order
- payment
- fulfillment

### Intelligence Plane
Owns read optimization:
- search
- notifications
- analytics

## Phase 1 Shape
Phase 1 should have:
- `edge-gateway`
- `iam-platform` via Keycloak
- `catalog-governance`
- `inventory-availability`
- `search-discovery`
- `search-service` for projection-backed product discovery and future search-provider isolation
- `promotion-service` for deterministic discount rule ownership and promotion admin workflows
- `payment-service` for isolated provider orchestration and webhook handling
- `shipping-service` for shipment lifecycle ownership, rule-based quoting, and future carrier integration
- `review-service` for moderated storefront product feedback and approved-only rating aggregation
- `commerce-core` modular monolith

## Architectural Principles
- domain-driven design
- API-first
- event-driven integration
- one write owner per aggregate
- strong bounded contexts
- observability first
- security first
- startup-pragmatic execution

## Critical Domain Rule
Catalog truth is global and governed.

Separation of responsibilities:
- catalog owns product identity
- inventory owns stock
- pricing owns commercial terms
- search owns projections only
- merchant/store domains own participation, not product truth

## Integration Model
- synchronous HTTP for user-facing commands and queries
- asynchronous events for cross-domain propagation
- transactional outbox for publishing
- no distributed transactions
- no direct DB writes across services

Payment-specific note:
- payment-service validates order identity/amount by synchronous order lookup
- payment-service owns provider state and webhook deduplication
- order-service does not persist provider-specific state

Purchase-flow note:
- checkout-service now owns the synchronous storefront purchase command path
- happy path is: cart validation -> stock reservation -> order creation -> payment confirmation -> order finalization -> notification dispatch
- order finalization uses a trusted internal order lifecycle endpoint instead of public admin routes
- notification dispatch resolves the customer UUID through customer-service before calling notification-service

Shipping-specific note:
- shipping-service validates order identity and shipment ownership by synchronous order lookup
- shipping-service owns shipment state, tracking references, and carrier refresh logic
- order-service does not persist carrier-specific shipment state in this slice

Promotion-specific note:
- promotion-service owns promotion definitions, code/coupon validation, and deterministic discount evaluation
- pricing-service remains responsible for base product price ownership
- checkout/cart callers consume promotion-service responses without promotion-service mutating cart/order state directly

Review-specific note:
- review-service owns customer review records, moderation state, submission spam signals, and rating summaries
- catalog-service remains the read-only source for product identity and listing availability
- catalog-service does not own or mutate review moderation state in this slice

Search-specific note:
- catalog-service remains the source of truth for product identity plus browse/admin product search
- search-service owns `/api/v1/search/**`, search projections, and the search-provider abstraction
- search-service reads canonical catalog data during rebuilds but does not become the owner of product truth

Frontend-compatibility note:
- catalog-service temporarily owns admin recommendation/merchandising control compatibility endpoints so `apps/admin-web` can keep its current pages without a legacy catch-all backend
- shipping-service temporarily owns merchant/store/service-area compatibility endpoints because active store coverage and fulfillment geography already sit nearest to shipping ownership
- unresolved legacy-only admin modules (`location`, `carousels`, `product submissions`, `recovery`) should not be re-homed casually; they still need an explicit extracted owner

## Persistence Model
- PostgreSQL for transactional truth
- Redis/Valkey for cache and rate limits
- Kafka-compatible broker for events
- OpenSearch for search and filters
- object storage for media

## Deployment Model
- Docker
- Kubernetes
- Spring Cloud Gateway
- Keycloak
- OpenTelemetry
- Prometheus + Grafana
- GitHub Actions + GitOps deployment

## Anti-Goals
Do not:
- rewrite everything at once
- split every domain into a service immediately
- use search as system of record
- duplicate products for merchant/store participation
- let undocumented chat history replace architecture docs
