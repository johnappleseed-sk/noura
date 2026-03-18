# Backend API

## Admin Authorization Endpoints
Base prefix: `/api/v1`

### Resolve admin capabilities
`GET /admin/capabilities`

Auth:
- Any authenticated session

Response `data` example:
```json
{
  "roles": ["ADMIN"],
  "capabilities": {
    "overview.dashboard": true,
    "commerce.catalog": true,
    "governance.rbac": true
  }
}
```

### Resolve admin role-permission matrix
`GET /admin/authorization/matrix`

Auth:
- `PERM_ROLES_READ` or `ADMIN`/`SUPER_ADMIN`

Response `data` includes:
- `version`
- `actionCatalog`
- `scopes`
- `roles` (with grants and capabilities)

### List permission catalog
`GET /admin/authorization/permissions`

Auth:
- `PERM_ROLES_READ` or `ADMIN`/`SUPER_ADMIN`

### List permission presets
`GET /admin/authorization/permission-presets`

Auth:
- `PERM_ROLES_READ` or `ADMIN`/`SUPER_ADMIN`

### List roles
`GET /admin/authorization/roles`

Auth:
- `PERM_ROLES_READ` or `ADMIN`/`SUPER_ADMIN`

### Create role
`POST /admin/authorization/roles`

Auth:
- `PERM_ROLES_CREATE` or `ADMIN`/`SUPER_ADMIN`

### Update role metadata
`PATCH /admin/authorization/roles/{roleId}`

Auth:
- `PERM_ROLES_UPDATE` or `ADMIN`/`SUPER_ADMIN`

### Replace role permissions
`PUT /admin/authorization/roles/{roleId}/permissions`

Auth:
- (`PERM_ROLES_UPDATE` or `PERM_ROLES_APPROVE`) or `ADMIN`/`SUPER_ADMIN`

### Apply role permission preset
`PUT /admin/authorization/roles/{roleId}/permission-presets/{presetCode}`

Auth:
- (`PERM_ROLES_UPDATE` or `PERM_ROLES_APPROVE`) or `ADMIN`/`SUPER_ADMIN`

### Deactivate role
`DELETE /admin/authorization/roles/{roleId}`

Auth:
- `PERM_ROLES_DELETE` or `ADMIN`/`SUPER_ADMIN`

### Read user role assignments
`GET /admin/authorization/users/{userId}/roles`

Auth:
- (`PERM_ROLES_READ` and `PERM_USERS_READ`) or `ADMIN`/`SUPER_ADMIN`

### Replace user role assignments
`PUT /admin/authorization/users/{userId}/roles`

Auth:
- ((`PERM_ROLES_UPDATE` or `PERM_ROLES_APPROVE`) and `PERM_USERS_UPDATE`) or `ADMIN`/`SUPER_ADMIN`

### Preview bulk user role assignments
`POST /admin/authorization/users/roles/bulk/preview`

Auth:
- ((`PERM_ROLES_UPDATE` or `PERM_ROLES_APPROVE`) and `PERM_USERS_UPDATE`) or `ADMIN`/`SUPER_ADMIN`

Response highlights:
- `requestedUsers`
- `resolvableUsers`
- `missingUsers`
- `changedUsers`
- `missingUserIds[]`
- `items[]` with `currentRoleCodes`, `proposedRoleCodes`, `rolesToAdd`, `rolesToRemove`

### Bulk replace user role assignments
`PUT /admin/authorization/users/roles/bulk`

Auth:
- ((`PERM_ROLES_UPDATE` or `PERM_ROLES_APPROVE`) and `PERM_USERS_UPDATE`) or `ADMIN`/`SUPER_ADMIN`

### List saved bulk assignment views
`GET /admin/authorization/users/roles/bulk/views`

Auth:
- ((`PERM_ROLES_UPDATE` or `PERM_ROLES_APPROVE`) and `PERM_USERS_UPDATE`) or `ADMIN`/`SUPER_ADMIN`

Response `data` fields:
- `id`
- `name`
- `query`
- `userIds[]`
- `roleCodes[]`
- `updatedAt`

### Upsert saved bulk assignment view
`POST /admin/authorization/users/roles/bulk/views`

Auth:
- ((`PERM_ROLES_UPDATE` or `PERM_ROLES_APPROVE`) and `PERM_USERS_UPDATE`) or `ADMIN`/`SUPER_ADMIN`

Request `data` fields:
- `name` (required, max 120)
- `query` (optional, max 255)
- `userIds[]` (optional, max 200)
- `roleCodes[]` (optional, max 30, validated as active assignable roles)

### Delete saved bulk assignment view
`DELETE /admin/authorization/users/roles/bulk/views/{viewId}`

Auth:
- ((`PERM_ROLES_UPDATE` or `PERM_ROLES_APPROVE`) and `PERM_USERS_UPDATE`) or `ADMIN`/`SUPER_ADMIN`

Notes:
- View ownership is actor-scoped.
- Returns `404 BULK_VIEW_NOT_FOUND` for unknown or non-owned IDs.

### Read RBAC audit logs
`GET /admin/authorization/audit-logs`

Auth:
- `PERM_AUDIT_LOGS_READ` or `PERM_ROLES_READ` or `ADMIN`/`SUPER_ADMIN`

Query params:
- `actionType` (optional)
- `entityType` (optional)
- `outcome` (optional)
- `query` (optional)
- `errorsOnly` (optional boolean)
- `occurredFrom` (optional ISO-8601 inclusive lower bound)
- `occurredTo` (optional ISO-8601 inclusive upper bound)
- `page`/`size`/`sortBy`/`direction`

### Export RBAC audit logs
`GET /admin/authorization/audit-logs/export`

Auth:
- `PERM_AUDIT_LOGS_EXPORT` or `ADMIN`/`SUPER_ADMIN`

Response:
- `text/csv` attachment (`admin-rbac-audit-logs.csv`)
- Includes audit columns and `payload_hash` integrity value

See detailed endpoint contract:
- [docs/api/admin-authorization-matrix.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/api/admin-authorization-matrix.md)

## Cart Service Endpoints
Base prefix: `/api/v1`

### Apply cart coupon
`POST /cart/coupon`

Behavior:
- validates the supplied coupon through `promotion-service` (`/api/v1/promotions/validate-code`)
- updates persisted cart totals (`discountAmount`, `totalAmount`) when eligible
- rejects invalid/ineligible coupons with stable cart error codes

### Remove cart coupon
`DELETE /cart/coupon`

Behavior:
- clears `couponCode` and recomputes totals deterministically

## Checkout Service Endpoints
Base prefix: `/api/v1`

### Direct checkout
`POST /checkout`

Behavior:
- validates cart lines, address ownership, pricing, and inventory availability
- requires a resolved `storeId` for inventory reservation
- creates the order in `PAYMENT_PENDING`
- creates and confirms a payment through `payment-service`
- finalizes the order to `PAID` when payment reaches `AUTHORIZED` or `CAPTURED`
- releases reservations and cancels the order when payment fails
- clears the cart and dispatches notification best-effort after success

Request highlights:
- `storeId`
- `addressId`
- `paymentMethod`
- `paymentProvider`
- `paymentProviderReference`
- `paymentAutoCapture`
- `couponCode`
- `idempotencyKey`

Response highlights:
- `order`
- `payment`
- `reservedStock`
- `idempotencyKey`
- `placedAt`

### Checkout preview
`POST /checkout/preview`

### Checkout validation
`POST /checkout/validate`

### Explicit place-order endpoint
`POST /checkout/place-order`

Behavior:
- same orchestration as direct checkout, but without the storefront-compatibility wrapper

## Payment Service Endpoints
Base prefix: `/api/v1`

### Create payment intent
`POST /payments/intents`

Behavior:
- validates order existence and actor ownership/admin access through `order-service`
- snapshots immutable `totalAmount` and `currencyCode` from the order
- persists an internal payment record before the caller starts confirmation

### Confirm payment
`POST /payments/{paymentId}/confirm`

Behavior:
- `AUTHORIZE` performs auth-only confirmation
- `CAPTURE` performs authorize+capture behavior through the selected provider adapter
- repeated confirms are safe for already-terminal or already-satisfied states

### Get payment by id
`GET /payments/{paymentId}`

### Get latest payment by order id
`GET /payments/order/{orderId}`

### Internal payment status update
`POST /internal/payments/status-update`

Behavior:
- trusted service/operator endpoint
- supports provider event correlation metadata when an upstream system already normalized the provider callback

### Provider webhook
`POST /payments/webhooks/{providerCode}`

Behavior:
- provider-generic route shape
- sandbox provider currently accepts deterministic JSON payloads
- duplicate deliveries are deduplicated by persisted provider event identity

Detailed contract:
- [docs/api/payment-service.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/api/payment-service.md)

## Shipping Service Endpoints
Base prefix: `/api/v1`

### Get shipping methods
`GET /shipping/methods`

Behavior:
- resolves available methods for a destination and cart snapshot
- supports optional carrier filtering
- uses the internal rule-based carrier in the current slice

### Create shipping quote
`POST /shipping/quotes`

Behavior:
- calculates one quote for a selected method
- rejects unsupported destination, method, or parcel combinations

### Create shipment
`POST /shipping/shipments`

Behavior:
- validates order existence and actor ownership/admin access through `order-service`
- snapshots the order shipping address into a shipment record
- persists one shipment record and carrier identifiers/tracking data
- replays the existing record when `(orderId, customerRef, idempotencyKey)` already exists
- keeps the first slice at one active shipment per order until split-shipment design exists

### Get shipment by id
`GET /shipping/shipments/{shipmentId}`

Query params:
- `refreshCarrier` (optional boolean)

### Get latest shipment by order id
`GET /shipping/shipments/order/{orderId}`

Query params:
- `refreshCarrier` (optional boolean)

### Internal shipment status update
`POST /internal/shipping/shipments/status-update`

Behavior:
- trusted service/operator endpoint
- applies shipment lifecycle transitions for warehouse, manual-ops, or future carrier callback flows
- updates tracking details, estimates, and failure reasons when provided

Detailed contract:
- [docs/api/shipping-service.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/api/shipping-service.md)

## Promotion Service Endpoints
Base prefix: `/api/v1`

### List active promotions
`GET /promotions/active`

Behavior:
- returns active, non-archived, date-valid promotions
- powers storefront/admin promotion discovery

### Validate promo code
`POST /promotions/validate-code`

Behavior:
- validates one promo code against current subtotal, segment, and item scope
- distinguishes `valid` from `eligible`
- returns stable machine-readable reason codes for rejection paths

### Evaluate cart discount
`POST /promotions/evaluate`

Behavior:
- evaluates automatic promotions and the supplied promo-code promotion together
- uses deterministic priority ordering
- respects `stackable=false`

### Create promotion
`POST /promotions`

Behavior:
- admin/marketing-protected mutation route
- also available under `POST /admin/promotions`

### List promotions
`GET /admin/promotions`

### Get promotion
`GET /admin/promotions/{promotionId}`

### Update promotion
`PATCH /admin/promotions/{promotionId}`

### Delete promotion
`DELETE /admin/promotions/{promotionId}`

### Admin evaluation
`POST /admin/promotions/evaluate`

Behavior:
- preserves the existing admin-dashboard contract
- reuses the same deterministic evaluation engine as the public evaluate endpoint

Detailed contract:
- [docs/api/promotion-service.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/api/promotion-service.md)

## Review Service Endpoints
Base prefix: `/api/v1`

### Get reviews by product
`GET /products/{productId}/reviews`

Behavior:
- returns `APPROVED` reviews by default
- moderators may pass `moderationStatus=PENDING|APPROVED|REJECTED`
- public callers cannot read pending or rejected reviews

### Submit review
`POST /products/{productId}/reviews`

Behavior:
- requires an authenticated customer subject
- validates the product through `catalog-service`
- allows one review per customer per product
- persists the review as `PENDING` until moderation occurs
- stores privacy-safe spam/moderation preparation fields (`submissionIpHash`, `submissionUserAgentHash`, `spamSignalsJson`)

### Get rating summary
`GET /products/{productId}/rating-summary`

Behavior:
- aggregates `averageRating`, `reviewCount`, and per-star counts from approved reviews only
- does not mutate catalog/product aggregates in the first slice

### Approve review
`POST /admin/reviews/{reviewId}/approve`

Behavior:
- requires admin/moderator privileges or trusted internal access
- marks the review `APPROVED`
- records moderation notes and audit metadata when supplied

### Reject review
`POST /admin/reviews/{reviewId}/reject`

Behavior:
- requires admin/moderator privileges or trusted internal access
- marks the review `REJECTED`
- keeps the review hidden from public reads and rating aggregates

Detailed contract:
- [docs/api/review-service.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/api/review-service.md)

## Search Service Endpoints
Base prefix: `/api/v1/search`

### Search products
`GET /search/products`

Query params:
- `q`, `keyword`, or `query`
- `categoryId` (optional UUID)
- `brandId` (optional UUID)
- `page` (optional, default `0`)
- `size` (optional, default `20`, max `100`)

Behavior:
- queries search-owned projection documents rather than canonical catalog tables
- returns an empty page for blank queries so this endpoint does not become a second catalog browse API
- supports optional category and brand filtering

### Predictive suggestions
`GET /search/predictive`

Query params:
- `q`
- `scope` (`all`, `products`, `brands`, `categories`, `stores`)

Behavior:
- serves product, brand, and category suggestions from indexed documents
- keeps store suggestions as a temporary read-through compatibility path

### Trend tags
`GET /search/trend-tags`

Behavior:
- derives discovery tags from trending indexed product documents
- falls back to stable default tags when the projection is empty
- exposes compatibility aliases `name` and `tag` alongside `value`

### Internal product index upsert
`POST /internal/search/index/products`

Behavior:
- trusted internal endpoint protected by `X-Internal-Api-Key`
- upserts one or more product documents into the search projection

### Internal product index rebuild
`POST /internal/search/index/products/rebuild`

Behavior:
- trusted internal endpoint protected by `X-Internal-Api-Key`
- rebuilds the search projection from canonical catalog source tables

### Internal product index delete
`DELETE /internal/search/index/products/{productId}`

Behavior:
- trusted internal endpoint protected by `X-Internal-Api-Key`
- removes one product document from the search projection

Detailed contract:
- [docs/api/search-service.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/api/search-service.md)

## Product Enrichment Endpoints
Base prefix: `/api/v1`

### Search existing products
`GET /products/search?q={query}`

Auth:
- `ADMIN`

Response `data` example:
```json
[
  {
    "id": "9a95dcad-8498-4e04-8b68-94cd12b1cd50",
    "name": "Noura Pro Headphones",
    "category": "Electronics",
    "description_missing": true,
    "barcode_missing": false,
    "qr_missing": true,
    "mirror_status": "UNMAPPED"
  }
]
```

Notes:
- Query supports product UUID exact match, name contains, and variant SKU contains.
- Results are deduplicated and capped.

### Get product details
`GET /products/{id}`

Response includes enrichment fields:
- `description`
- `targetAudience`
- `barcode`
- `qrCode`

### Generate all missing fields
`POST /products/{id}/generate-missing`

Auth:
- `ADMIN`

Behavior:
- Generates only missing values for description/barcode/qr.
- Persists updates in commerce product.
- Queues mirror sync job for changed fields.

Response `data` example:
```json
{
  "id": "9a95dcad-8498-4e04-8b68-94cd12b1cd50",
  "product_name": "Noura Pro Headphones",
  "description": "...",
  "barcode": "1234567890128",
  "qr_code": "https://store.example.com/products/9a95dcad-8498-4e04-8b68-94cd12b1cd50",
  "barcode_image_url": "/api/v1/products/9a95dcad-8498-4e04-8b68-94cd12b1cd50/barcode-image",
  "qr_image_url": "/api/v1/products/9a95dcad-8498-4e04-8b68-94cd12b1cd50/qr-image",
  "description_generated": true,
  "barcode_generated": true,
  "qr_generated": true,
  "mirror_status": "PENDING",
  "mirror_warning": null
}
```

### Generate description only
`POST /products/{id}/generate-description`

Auth:
- `ADMIN`

Behavior:
- Generates description only when missing.

### Generate barcode only
`POST /products/{id}/generate-barcode`

Auth:
- `ADMIN`

Behavior:
- Generates unique EAN-13 barcode only when missing.

### Generate QR only
`POST /products/{id}/generate-qr`

Auth:
- `ADMIN`

Behavior:
- Generates QR payload only when missing.

### Render barcode image
`GET /products/{id}/barcode-image`

Auth:
- `ADMIN`

Response:
- `image/png`
- `404` when barcode is missing.

### Render QR image
`GET /products/{id}/qr-image`

Auth:
- `ADMIN`

Response:
- `image/png`
- `404` when QR value is missing.

## Error Cases
Common errors for enrichment endpoints:
- Product not found/inactive
- Access denied for non-admin role
- Barcode generation failure after collision retries
- External LLM failure (auto-fallback to template description)
- Mirror mapping missing (`mirror_status=BLOCKED_MAPPING` + warning)
