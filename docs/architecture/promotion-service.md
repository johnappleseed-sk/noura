# Promotion Service Architecture

## Purpose

`promotion-service` isolates promotion state and deterministic discount evaluation from the broader pricing/order/checkout flow.

This extraction is intentionally narrower than a full offer platform:

- promotion definitions and admin CRUD move into one service
- cart/checkout callers get a stable evaluation API
- the rule model stays deterministic and archived-code-compatible
- a generic rule engine is intentionally deferred

## Reuse baseline

The service modernizes two archived promotion implementations:

- the richer promotion aggregate from `archive/legacy-monolith/backend-monolith/src/main/java/com/noura/platform/domain/entity/Promotion.java`
- the deterministic promotion evaluation flow from `archive/legacy-monolith/backend-monolith/src/main/java/com/noura/platform/service/impl/PromotionRuleEngineServiceImpl.java`

The extracted service preserves those semantics while:

- moving persistence into its own schema/migration
- using explicit API envelopes and request-context resolution
- preventing ambiguous identifier reuse between `code` and `couponCode`

## Domain model

### Promotion aggregate

`PromotionRecord` owns:

- name / description
- promotion `type`
- optional `code`
- optional `couponCode`
- conditions JSON
- date validity
- active / archived flags
- stackability
- priority
- usage counters and limit placeholders
- optional exact-match `customerSegment`

### Promotion application mappings

`PromotionApplicationRecord` constrains a promotion to one or more scopes:

- `PRODUCT`
- `CATEGORY`
- `VARIANT`
- `COLLECTION`

The v1 collection path remains deterministic by relying on `conditions.collectionProductIds`.

## Evaluation model

Evaluation is a simple ordered pass, not a dynamic rule graph:

1. Load active, non-archived promotions.
2. Filter by date window and total usage limit.
3. Sort by `priority` descending.
4. Match the requested promo code against `code` or `couponCode`.
5. Allow automatic promotions with no explicit code to participate in the same pass.
6. Enforce customer segment and scope mappings.
7. Calculate the type-specific discount.
8. Stop after the first successful `stackable=false` promotion.

This keeps promotion behavior easy to reason about in logs, tests, and operator runbooks.

## Rule types

Primary first-slice business rules:

- `PERCENTAGE`
- `FIXED`

Archived deterministic rules retained because they already existed and were worth reusing:

- `FREE_SHIPPING`
- `CART_THRESHOLD_DISCOUNT`
- `BUY_X_GET_Y`
- `PRODUCT_BUNDLE_DISCOUNT`

The service does not attempt to generalize these into a user-authored DSL yet.

## Boundary decisions

### Why separate from pricing-service

- product base pricing and promotional discount policy change at different cadences
- admin promotion workflows already act like their own operating surface
- checkout/cart integrations need a dedicated discount-evaluation contract
- future promotion redemption tracking is a different write concern from list-price management

### What stays outside promotion-service

- price-list and product-price ownership remain in `pricing-service`
- cart ownership remains in `cart-service`
- checkout orchestration remains in `checkout-service`
- order confirmation remains in `order-service`

`promotion-service` evaluates discounts but does not mutate those downstream aggregates.

## Identifier strategy

`code` and `couponCode` are treated as one logical lookup namespace.

Reason:

- clients validate or evaluate using a single promo-code input
- allowing the same value to exist once as `code` and once as `couponCode` would make validation ambiguous

The service therefore rejects cross-field duplication even though the database uniqueness indexes are field-local.

## Limitations and follow-up

- add redemption history so `usageLimitPerCustomer` becomes enforceable
- add order/checkout integration that increments usage counts on successful redemption, not on preview/evaluation
- add event publication or outbox integration for promotion-applied analytics and order coordination
- decide whether advanced rules belong in a richer internal policy model or an external offer engine later
