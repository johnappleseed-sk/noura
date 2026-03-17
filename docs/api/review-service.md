# Review Service API

Base prefix: `/api/v1`

Legacy aliases without `/v1` are preserved for compatibility.

## Summary

`review-service` owns:

- product review submission
- product-scoped review reads
- approved-rating aggregation
- moderation status transitions
- moderation/spam-ready submission metadata

The first version intentionally stays easy to reason about:

- one review per customer per product
- no public visibility before moderation
- no product aggregate back-write into catalog
- no generic comments/reputation engine

## Review lifecycle

- `PENDING`: created state for every new review
- `APPROVED`: visible in public lists and included in rating summaries
- `REJECTED`: hidden from public lists and excluded from rating summaries

Terminal moderation states in v1:

- `APPROVED`
- `REJECTED`

Moderators may reverse a previous decision through the approve/reject endpoints; the latest status is treated as the current source of truth.

## Public/storefront-compatible endpoints

### Get reviews by product

`GET /products/{productId}/reviews`

Query params:

- `moderationStatus` optional and moderator-only

Behavior:

- public callers receive `APPROVED` reviews only
- moderators may request `PENDING`, `APPROVED`, or `REJECTED`
- responses are ordered by newest submission first

Response `data` example:

```json
[
  {
    "id": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
    "productId": "11111111-1111-1111-1111-111111111111",
    "customerName": "Noura Shopper",
    "rating": 5,
    "title": "Excellent",
    "comment": "Quality matched expectations.",
    "moderationStatus": "APPROVED",
    "createdAt": "2026-03-17T10:15:30Z",
    "moderatedAt": "2026-03-17T11:00:00Z"
  }
]
```

### Submit review

`POST /products/{productId}/reviews`

Request:

```json
{
  "rating": 5,
  "title": "Excellent",
  "comment": "Quality matched expectations."
}
```

Behavior:

- requires an authenticated customer subject
- validates the product through `catalog-service`
- rejects duplicate submissions from the same customer for the same product
- persists the review as `PENDING`
- stores hashed transport metadata for later spam/moderation tooling

Response `data` example:

```json
{
  "id": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  "productId": "11111111-1111-1111-1111-111111111111",
  "customerName": "Noura Shopper",
  "rating": 5,
  "title": "Excellent",
  "comment": "Quality matched expectations.",
  "moderationStatus": "PENDING",
  "createdAt": "2026-03-17T10:15:30Z",
  "moderatedAt": null
}
```

Common error codes:

- `AUTHENTICATION_REQUIRED`
- `PRODUCT_NOT_FOUND`
- `PRODUCT_REVIEW_UNAVAILABLE`
- `REVIEW_ALREADY_EXISTS`
- `CATALOG_SERVICE_UNREACHABLE`
- `CATALOG_SERVICE_ERROR`

### Get rating summary

`GET /products/{productId}/rating-summary`

Behavior:

- aggregates approved reviews only
- returns average rating, total approved review count, and per-star buckets
- returns zeros when a product has no approved reviews yet

Response `data` example:

```json
{
  "productId": "11111111-1111-1111-1111-111111111111",
  "averageRating": 4.33,
  "reviewCount": 3,
  "fiveStarCount": 1,
  "fourStarCount": 2,
  "threeStarCount": 0,
  "twoStarCount": 0,
  "oneStarCount": 0
}
```

## Admin moderation endpoints

Admin moderation requires internal trust or moderator/admin-like roles resolved through the gateway-forwarded request context.

### Approve review

`POST /admin/reviews/{reviewId}/approve`

Request:

```json
{
  "moderationNotes": "Approved after manual moderation review."
}
```

Behavior:

- sets `moderationStatus=APPROVED`
- records `moderatedAt`, `moderatedBy`, and `approvedAt`
- clears `rejectedAt`

### Reject review

`POST /admin/reviews/{reviewId}/reject`

Request:

```json
{
  "moderationNotes": "Rejected for spam or duplicate content."
}
```

Behavior:

- sets `moderationStatus=REJECTED`
- records `moderatedAt`, `moderatedBy`, and `rejectedAt`
- clears `approvedAt`

## Spam/moderation-ready fields

The first slice stores moderation-ready data without retaining raw transport values:

- `submissionIpHash`
- `submissionUserAgentHash`
- `spamSignalsJson`

This keeps moderation tooling extensible while avoiding raw IP/user-agent persistence in the current slice.

## Known limitations

- No review edit/delete flow exists yet
- No automated spam classification exists yet
- No review media/attachments exist yet
- No catalog or search projection sync exists yet for aggregate ratings
- No admin queue/list endpoint exists yet beyond product-scoped filtering
