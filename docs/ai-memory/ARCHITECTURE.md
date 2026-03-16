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
