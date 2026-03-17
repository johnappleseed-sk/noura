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
