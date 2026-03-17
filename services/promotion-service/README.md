# promotion-service

Production promotion abstraction service for admin promotion CRUD, promo-code validation, and deterministic cart discount evaluation.

## Exposed endpoints

- `GET /api/v1/promotions/active` (legacy alias: `/api/promotions/active`)
- `POST /api/v1/promotions/validate-code` (legacy alias: `/api/promotions/validate-code`)
- `POST /api/v1/promotions/evaluate` (legacy aliases: `/api/promotions/evaluate`, `/api/v1/promotions/evaluate-cart`, `/api/promotions/evaluate-cart`)
- `POST /api/v1/promotions` (legacy alias: `/api/promotions`)
- `GET /api/v1/admin/promotions` (legacy alias: `/api/admin/promotions`)
- `GET /api/v1/admin/promotions/{promotionId}` (legacy alias: `/api/admin/promotions/{promotionId}`)
- `PATCH /api/v1/admin/promotions/{promotionId}` (legacy alias: `/api/admin/promotions/{promotionId}`)
- `DELETE /api/v1/admin/promotions/{promotionId}` (legacy alias: `/api/admin/promotions/{promotionId}`)
- `POST /api/v1/admin/promotions/evaluate` (legacy alias: `/api/admin/promotions/evaluate`)

## Scope (v1)

- Owns promotion records, code/coupon identifiers, date windows, scope mappings, and deterministic discount rule conditions.
- Supports percentage and fixed discounts as the main first-slice use cases.
- Also carries forward deterministic archived rule types already present in the monolith:
  - `FREE_SHIPPING`
  - `CART_THRESHOLD_DISCOUNT`
  - `BUY_X_GET_Y`
  - `PRODUCT_BUNDLE_DISCOUNT`
- Returns normalized discount evaluation results for cart and checkout flows without introducing a generic rules platform.

## Reuse baseline

This extraction reuses and modernizes the archived promotion model and rule evaluation concepts from:

- `archive/legacy-monolith/backend-monolith/src/main/java/com/noura/platform/domain/entity/Promotion.java`
- `archive/legacy-monolith/backend-monolith/src/main/java/com/noura/platform/service/impl/PromotionAdminServiceImpl.java`
- `archive/legacy-monolith/backend-monolith/src/main/java/com/noura/platform/service/impl/PromotionRuleEngineServiceImpl.java`

The first service slice keeps those semantics deterministic instead of inventing a separate offer engine.

## Deterministic evaluation model

- Promotions are evaluated in descending `priority`.
- A supplied promo code matches `code` or `couponCode`.
- Automatic promotions with no `code`/`couponCode` still participate when a promo code is supplied.
- Eligibility checks run in this order:
  - archived / active state
  - start / end date validity
  - total usage limit
  - customer segment exact match
  - scope mapping match (`PRODUCT`, `CATEGORY`, `VARIANT`, `COLLECTION`)
  - type-specific condition checks
- `stackable=false` short-circuits evaluation after the first successful application.

## Identifier rules

- `code` and `couponCode` share one uniqueness namespace in the service layer.
- This avoids ambiguous promo-code validation when one promotion uses a value as `code` and another uses the same value as `couponCode`.

## Known limitations

- No redemption ledger exists yet, so `usageLimitPerCustomer` is stored but not enforced.
- No rule-builder UI or generic external rule engine exists yet.
- No order/checkout event publication is implemented yet for promotion application tracking.
- Collection applicability depends on deterministic `collectionProductIds` condition data rather than a dedicated catalog lookup.

## Local run

```bash
cd services/promotion-service
mvn spring-boot:run
```

Required environment variables:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

Optional environment variables:

- `SERVER_PORT` (default `8080`)
- `APP_INTERNAL_API_KEY`

See:

- [docs/api/promotion-service.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/api/promotion-service.md)
- [docs/architecture/promotion-service.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/architecture/promotion-service.md)
