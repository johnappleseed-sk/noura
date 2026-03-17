# Promotion Service API

Base prefix: `/api/v1`

Legacy aliases without `/v1` are preserved for compatibility.

## Summary

`promotion-service` owns:

- promotion records
- promo code and coupon code lookup
- deterministic cart discount evaluation
- admin promotion CRUD

The first version intentionally stays easy to reason about:

- no external rule engine
- no hidden side effects
- no order mutation
- no per-customer redemption ledger yet

## Public/storefront-compatible endpoints

### List active promotions

`GET /promotions/active`

Returns currently active, non-archived, date-valid, usage-available promotions.

### Validate promo code

`POST /promotions/validate-code`

Request:

```json
{
  "promoCode": "SAVE10",
  "subtotal": 50.00,
  "customerSegment": "vip",
  "items": [
    {
      "productId": "11111111-1111-1111-1111-111111111111",
      "categoryId": "22222222-2222-2222-2222-222222222222",
      "variantId": "33333333-3333-3333-3333-333333333333",
      "quantity": 2,
      "unitPrice": 25.00
    }
  ]
}
```

Behavior:

- `valid=true` means the supplied code matched one promotion record.
- `eligible=true` means the matched promotion is currently applicable to the supplied cart snapshot.
- `evaluation` is returned only when the matched promotion is eligible.

Response `data` example:

```json
{
  "valid": true,
  "eligible": true,
  "reasonCode": "PROMOTION_VALID",
  "reasonMessage": "Promotion is eligible for the supplied cart snapshot",
  "promotion": {
    "id": "44444444-4444-4444-4444-444444444444",
    "name": "VIP Spring Sale",
    "code": "SPRING-SALE",
    "description": "Ten percent off selected products",
    "type": "PERCENTAGE",
    "couponCode": "SAVE10",
    "conditions": {
      "percent": 10
    },
    "startDate": "2026-03-17T00:00:00Z",
    "endDate": "2026-03-31T23:59:59Z",
    "active": true,
    "stackable": true,
    "priority": 50,
    "usageLimitTotal": 1000,
    "usageLimitPerCustomer": null,
    "usageCount": 0,
    "customerSegment": "vip",
    "archived": false,
    "discountPercent": 10.00,
    "discountAmount": null,
    "applications": [
      {
        "applicableEntityType": "PRODUCT",
        "applicableEntityId": "11111111-1111-1111-1111-111111111111"
      }
    ]
  },
  "evaluation": {
    "subtotal": 50.00,
    "discountAmount": 10.00,
    "discountedSubtotal": 40.00,
    "freeShipping": false,
    "appliedPromotionIds": [
      "44444444-4444-4444-4444-444444444444"
    ],
    "appliedPromotionCodes": [
      "SPRING-SALE"
    ]
  }
}
```

Common reason codes:

- `PROMO_CODE_NOT_FOUND`
- `PROMOTION_ARCHIVED`
- `PROMOTION_INACTIVE`
- `PROMOTION_NOT_STARTED`
- `PROMOTION_EXPIRED`
- `PROMOTION_USAGE_LIMIT_REACHED`
- `PROMOTION_CUSTOMER_SEGMENT_MISMATCH`
- `PROMOTION_SCOPE_MISMATCH`
- `PROMOTION_CART_NOT_ELIGIBLE`

### Evaluate cart discount

`POST /promotions/evaluate`

Request:

```json
{
  "subtotal": 80.00,
  "promoCode": "SAVE10",
  "customerSegment": "vip",
  "items": [
    {
      "productId": "11111111-1111-1111-1111-111111111111",
      "categoryId": "22222222-2222-2222-2222-222222222222",
      "variantId": "33333333-3333-3333-3333-333333333333",
      "quantity": 2,
      "unitPrice": 40.00
    }
  ]
}
```

Behavior:

- applies automatic promotions and the supplied promo-code promotion together
- respects priority ordering and `stackable=false`
- returns a normalized discount summary for checkout/cart callers

Response `data` example:

```json
{
  "subtotal": 80.00,
  "discountAmount": 13.00,
  "discountedSubtotal": 67.00,
  "freeShipping": false,
  "appliedPromotionIds": [
    "55555555-5555-5555-5555-555555555555",
    "44444444-4444-4444-4444-444444444444"
  ],
  "appliedPromotionCodes": [
    "SAVE5",
    "SPRING-SALE"
  ]
}
```

## Admin endpoints

Admin mutation/read access requires internal trust or admin/marketing-like roles resolved through the gateway-forwarded request context.

### Create promotion

`POST /promotions`

Also available as `POST /admin/promotions`.

Request:

```json
{
  "name": "VIP Spring Sale",
  "type": "PERCENTAGE",
  "code": "SPRING-SALE",
  "description": "Ten percent off selected products",
  "couponCode": "SAVE10",
  "conditions": {
    "percent": 10
  },
  "startDate": "2026-03-17T00:00:00Z",
  "endDate": "2026-03-31T23:59:59Z",
  "active": true,
  "stackable": true,
  "priority": 50,
  "usageLimitTotal": 1000,
  "usageLimitPerCustomer": null,
  "customerSegment": "vip",
  "archived": false,
  "applications": [
    {
      "applicableEntityType": "PRODUCT",
      "applicableEntityId": "11111111-1111-1111-1111-111111111111"
    }
  ]
}
```

Notes:

- `type=PERCENTAGE` requires `conditions.percent`
- `type=FIXED` requires `conditions.amount`
- `code` and `couponCode` are checked against one combined uniqueness namespace

### List promotions

`GET /admin/promotions`

Query params:

- `query` optional substring match on `name`, `code`, or `couponCode`
- `active` optional boolean
- `archived` optional boolean

### Get promotion

`GET /admin/promotions/{promotionId}`

### Update promotion

`PATCH /admin/promotions/{promotionId}`

Uses the same request shape as create.

### Delete promotion

`DELETE /admin/promotions/{promotionId}`

Deletes the promotion record permanently.

Archiving via `PATCH` is still the safer operational path when history should remain queryable.

### Admin evaluation endpoint

`POST /admin/promotions/evaluate`

Uses the same request and response shape as the public cart-evaluation endpoint, but preserves the existing admin-dashboard contract.

## Supported rule types

Primary v1 business scope:

- `PERCENTAGE`
- `FIXED`

Deterministic archived rule types carried forward:

- `FREE_SHIPPING`
- `CART_THRESHOLD_DISCOUNT`
- `BUY_X_GET_Y`
- `PRODUCT_BUNDLE_DISCOUNT`

## Known limitations

- `usageLimitPerCustomer` is stored but not enforced yet.
- Promotion usage counts are not incremented by evaluation; a later redemption or order-confirmation flow should own that mutation.
- Collection applicability depends on `conditions.collectionProductIds` rather than a real-time catalog collection lookup.
