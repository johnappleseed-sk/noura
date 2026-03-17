# review-service

Production review abstraction service for storefront product reviews, approved-rating aggregation, and admin moderation.

## Exposed endpoints

- `GET /api/v1/products/{productId}/reviews` (legacy alias: `/api/products/{productId}/reviews`)
- `POST /api/v1/products/{productId}/reviews` (legacy alias: `/api/products/{productId}/reviews`)
- `GET /api/v1/products/{productId}/rating-summary` (legacy alias: `/api/products/{productId}/rating-summary`)
- `POST /api/v1/admin/reviews/{reviewId}/approve` (legacy alias: `/api/admin/reviews/{reviewId}/approve`)
- `POST /api/v1/admin/reviews/{reviewId}/reject` (legacy alias: `/api/admin/reviews/{reviewId}/reject`)

## Scope (v1)

- Owns product review records, moderation status, moderation audit fields, and rating aggregates.
- Supports one review per customer per product.
- Defaults new reviews to `PENDING` until an admin/moderator approves or rejects them.
- Returns only `APPROVED` reviews to public callers.
- Allows moderators to query `/reviews?moderationStatus=PENDING|APPROVED|REJECTED`.
- Calculates rating summaries from approved reviews only.

## Reuse baseline

This extraction reuses and modernizes the archived review flow from:

- `archive/legacy-monolith/backend-monolith/src/main/java/com/noura/platform/domain/entity/ProductReview.java`
- `archive/legacy-monolith/backend-monolith/src/main/java/com/noura/platform/controller/ProductController.java`
- `archive/legacy-monolith/backend-monolith/src/main/java/com/noura/platform/service/impl/ProductServiceImpl.java`

The archived logic was intentionally thin, so the extracted service preserves that simplicity while adding moderation and spam-ready fields instead of introducing a full comment/reputation platform.

## Moderation model

- `PENDING`: hidden from storefront lists and rating aggregates
- `APPROVED`: visible to storefront and counted in aggregates
- `REJECTED`: hidden from storefront and excluded from aggregates

Moderation writes also persist:

- `moderatedAt`
- `moderatedBy`
- `moderationNotes`
- `approvedAt`
- `rejectedAt`

Submission records also keep privacy-safe spam/moderation preparation fields:

- `submissionIpHash`
- `submissionUserAgentHash`
- `spamSignalsJson`

## Known limitations

- No review edit/delete endpoint exists yet.
- No automated spam classifier or reputation scoring exists yet.
- No product aggregate back-write is performed to `catalog-service`; rating summaries are computed in `review-service`.
- No attachment/media support exists yet for review submissions.
- No admin queue/list endpoint exists yet beyond product-scoped filtering.

## Local run

```bash
cd services/review-service
mvn spring-boot:run
```

Required environment variables:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

Optional environment variables:

- `SERVER_PORT` (default `8080`)
- `APP_INTERNAL_API_KEY`
- `CATALOG_SERVICE_BASE_URL`
- `CATALOG_SERVICE_INTERNAL_API_KEY`

See:

- [docs/api/review-service.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/api/review-service.md)
- [docs/architecture/review-service.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/architecture/review-service.md)
