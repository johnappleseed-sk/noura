# Review Service Architecture

## Purpose

`review-service` isolates storefront customer feedback and moderation state from catalog product identity ownership.

This extraction is intentionally narrower than a full reputation platform:

- review submissions and moderation move into one service
- storefront gets a stable product-review contract
- moderation visibility stays explicit and deterministic
- spam/reputation intelligence is intentionally deferred

## Reuse baseline

The service modernizes the archived review flow from:

- `archive/legacy-monolith/backend-monolith/src/main/java/com/noura/platform/domain/entity/ProductReview.java`
- `archive/legacy-monolith/backend-monolith/src/main/java/com/noura/platform/controller/ProductController.java`
- `archive/legacy-monolith/backend-monolith/src/main/java/com/noura/platform/service/impl/ProductServiceImpl.java`

The archived implementation was intentionally thin: save reviews, list reviews, and recalculate product averages.

The extracted service preserves that simplicity while adding:

- explicit moderation states
- moderation audit fields
- privacy-safe spam/moderation preparation fields
- a standalone rating-summary API

## Domain model

### Review aggregate

`ProductReviewRecord` owns:

- `productId`
- `customerRef`
- `customerName`
- `rating`
- `title`
- `comment`
- `moderationStatus`
- `moderationNotes`
- `moderatedAt`
- `moderatedBy`
- `approvedAt`
- `rejectedAt`
- `submissionIpHash`
- `submissionUserAgentHash`
- `spamSignalsJson`

The v1 schema enforces one review per `(productId, customerRef)`.

### Moderation status model

- `PENDING`
- `APPROVED`
- `REJECTED`

Rules:

1. Every new review starts as `PENDING`.
2. Public reads and rating aggregates include `APPROVED` only.
3. Moderators can filter reads by explicit moderation status.
4. Moderators may change a previous decision by calling approve/reject again; the latest stored status is authoritative.

## Aggregation model

Rating summaries are computed from approved reviews inside `review-service`.

Why this stays local in v1:

- moderation status is the deciding factor for whether a review counts
- recomputing from approved reviews is simple and deterministic
- it avoids coupling catalog writes to review moderation flows too early

The current design intentionally does not push `averageRating` or `reviewCount` back into `catalog-service`.

## Integration boundaries

### Why not keep reviews in catalog-service

- catalog owns product identity and listing data
- review moderation has different operational actors and workflows
- spam handling and future reputation features would otherwise expand catalog responsibilities

### Catalog integration

- `review-service` validates product identity and activity through synchronous read-only `catalog-service` lookups during review submission
- no cross-service database writes occur
- rating aggregation stays inside `review-service`

This keeps the boundary clean:

- `catalog-service` owns product truth
- `review-service` owns customer feedback truth

## Spam/moderation-ready design

The first slice stores only hashed transport metadata:

- IP address hash
- user-agent hash
- structured `spamSignalsJson`

This is enough to support later:

- rate limiting
- heuristic spam detection
- moderator tooling
- abuse pattern analysis

without prematurely introducing raw telemetry retention or a classifier service.

## API shape

The current service exposes:

- product-scoped public review reads
- product-scoped review submission
- product-scoped rating summary reads
- admin moderation approve/reject commands

The public paths deliberately remain product-centric:

- `/api/v1/products/{productId}/reviews`
- `/api/v1/products/{productId}/rating-summary`

This preserves storefront compatibility while moving the write owner behind the gateway.

## Limitations and follow-up

- add admin moderation queue/list/search endpoints
- add review update/delete flows or explicit replacement behavior
- add spam heuristics, rate limiting, and reputation signals
- decide whether approved rating aggregates should project into catalog/search read models
- add review attachments/media only if moderation and storage policy are ready
