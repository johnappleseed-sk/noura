# Recent Changes

## 2026-03-18 - cross-service integration pass hardening

### Task
- Performed an integration hardening pass across gateway, cart, promotion, notification, payment, and shipping boundaries.

### Why
- Gateway did not expose payment/shipping APIs, which blocked ingress-level access.
- Storefront/admin compatibility paths expected `/api/v1/cart/coupon`, but cart-service had no coupon command surface.
- Notification API envelopes were not aligned with the shared correlation-aware response contract.
- Promotion rounding differed from the other transaction services.

### Key files touched
- `apps/api-gateway/src/main/resources/application.yml`
- `services/cart-service/src/main/java/com/noura/cart/controller/CartController.java`
- `services/cart-service/src/main/java/com/noura/cart/service/CartService.java`
- `services/cart-service/src/main/java/com/noura/cart/service/impl/CartServiceImpl.java`
- `services/cart-service/src/main/java/com/noura/cart/integration/PromotionGateway.java`
- `services/cart-service/src/main/java/com/noura/cart/integration/client/PromotionServiceClient.java`
- `services/notification-service/src/main/java/com/noura/notification/common/ApiResponse.java`
- `services/notification-service/src/main/java/com/noura/notification/config/RequestCorrelationFilter.java`
- `services/promotion-service/src/main/java/com/noura/promotion/service/impl/PromotionServiceImpl.java`

### Architecture decisions
- `api-gateway` is now the canonical ingress for payment/shipping public APIs and readiness probes.
- Coupon application remains cart-owned while coupon validity/eligibility logic stays promotion-owned through synchronous promotion-service validation.
- Cart totals recomputation now treats coupon failures as strict for explicit coupon-apply commands and non-strict for background cart recomputation paths.
- Notification now follows the same response envelope semantics as other services (`correlationId`, validation-error map, and request correlation propagation).
- Promotion money normalization was aligned to the 4-decimal transaction contract used by cart/order/checkout/pricing/payment/shipping.

### Integration notes
- New cart coupon routes:
  - `POST /api/v1/cart/coupon`
  - `DELETE /api/v1/cart/coupon`
- Cart coupon validation calls `promotion-service` endpoint `/api/v1/promotions/validate-code`.
- Gateway now routes:
  - `/api/v1/payments/**` and `/api/payments/**`
  - `/api/v1/shipping/**` and `/api/shipping/**`

### Known caveats
- Checkout still uses a `NoopPaymentGateway`; checkout-to-payment orchestration remains deferred.
- Shipping orchestration is still not triggered automatically during checkout placement.
- Status spelling is still domain-specific (`CANCELED` in payment vs `CANCELLED` in order/shipping); mapping harmonization is pending.

### Follow-up work
- Add checkout orchestration integration for payment intent creation/confirmation and shipment bootstrapping.
- Decide whether to standardize cancellation enum spelling platform-wide with compatibility aliases.
- Add integration tests that traverse gateway -> cart coupon -> promotion validation -> checkout totals.

## 2026-03-18 - search-service extraction completed

### Task
- Implemented `search-service` as the standalone discovery boundary with projection-backed product search, predictive suggestions, trend tags, and internal indexing operations.

### Why
- The audit showed `catalog-service` already owns product truth and browse/admin product search, but `/api/v1/search/**` already behaves like a separate discovery contract.
- The platform needed a real search boundary now without duplicating catalog ownership or introducing OpenSearch too early.

### Key files touched
- `services/search-service/src/main/java/com/noura/search/controller/SearchPublicController.java`
- `services/search-service/src/main/java/com/noura/search/controller/InternalSearchIndexController.java`
- `services/search-service/src/main/java/com/noura/search/provider/ProductSearchIndexProvider.java`
- `services/search-service/src/main/java/com/noura/search/provider/PostgresProductSearchIndexProvider.java`
- `services/search-service/src/main/resources/db/migration/V1__search_projection_foundation.sql`
- `docs/api/search-service.md`
- `docs/architecture/search-service.md`

### Architecture decisions
- `catalog-service` remains the source of truth for product identity plus browse/admin product-search flows.
- `search-service` now owns the canonical `/api/v1/search/**` discovery surface.
- Runtime reads use the search-owned `search_product_documents` projection table rather than direct catalog-table reads.
- The provider boundary reuses the archived search adapter concept so PostgreSQL can be replaced with OpenSearch later.
- Blank product search keywords return an empty page so `search-service` does not silently become a second browse API.

### Indexing model
- Internal indexing APIs live under `/internal/search/index/**`.
- Rebuilds currently read canonical source tables through read-only JPA mappings and repopulate the projection table.
- Internal indexing endpoints are protected by `X-Internal-Api-Key`.
- Store suggestions remain a temporary read-through compatibility path until store discovery gets its own projection.

### Integration notes
- `apps/api-gateway` can continue routing `/api/v1/search/**` and `/api/search/**` to `search-service`
- `apps/admin-web` control-center endpoint catalog now includes `GET /api/v1/search/products`
- `catalog-service` still contains transitional duplicate predictive/trend code, but the canonical boundary is now documented as `search-service`
- A frontend audit of `storefront-web` confirmed active reliance on `/api/v1/search/predictive` and `/api/v1/search/trend-tags`, so compatibility aliases were added to reduce client churn

### Known caveats
- No OpenSearch provider exists yet
- No event-bus or outbox-driven indexing flow exists yet
- No dedicated faceting or filter-aggregation API exists yet
- Store suggestions are not projected yet

### Follow-up work
- Publish incremental index events from catalog/review/merchandising changes
- Add an OpenSearch adapter behind `ProductSearchIndexProvider`
- Decide when catalog-side duplicate predictive/trend endpoints can be retired
- Add richer search ranking and facet aggregation once discovery requirements justify it

## 2026-03-17 - review-service extraction completed

### Task
- Implemented `review-service` with storefront review submission, public product review reads, approved-only rating aggregation, and admin approve/reject moderation actions.

### Why
- Review moderation and rating visibility are a different operational concern from catalog identity ownership.
- The platform needed a stable review boundary before adding richer spam tooling, reputation scoring, or admin moderation queues.

### Key files touched
- `services/review-service/src/main/java/com/noura/review/controller/ReviewController.java`
- `services/review-service/src/main/java/com/noura/review/service/impl/ReviewServiceImpl.java`
- `services/review-service/src/main/resources/db/migration/V1__review_foundation.sql`
- `apps/api-gateway/src/main/resources/application.yml`
- `docs/api/review-service.md`
- `docs/architecture/review-service.md`

### Architecture decisions
- `review-service` owns review records, moderation state, moderation audit fields, and rating aggregates.
- The extraction reuses the archived `ProductReview` flow but keeps the first slice intentionally deterministic rather than introducing a generic comment or reputation platform.
- Product identity is validated through synchronous read-only `catalog-service` lookups on submission.
- Rating aggregates are computed from approved reviews inside `review-service` and are not pushed back into catalog/product records yet.

### Moderation model
- New reviews default to `PENDING`.
- Storefront review lists and rating summaries include `APPROVED` reviews only.
- Moderators can explicitly filter product-scoped review reads by `PENDING`, `APPROVED`, or `REJECTED`.
- Moderation actions persist `moderatedAt`, `moderatedBy`, `moderationNotes`, `approvedAt`, and `rejectedAt`.

### Integration notes
- `apps/api-gateway` now routes `/api/v1/products/{productId}/reviews`, `/api/v1/products/{productId}/rating-summary`, and `/api/v1/admin/reviews/**` to `review-service`
- `apps/admin-web` control-center endpoint catalog now exposes review-service endpoints explicitly
- Storefront paths remain stable because the public review contract stays under `/api/v1/products/{productId}/...`

### Known caveats
- No review edit/delete API exists yet
- No automated spam scoring or reputation model exists yet
- No catalog-side aggregate projection is written yet
- No global admin moderation queue endpoint exists yet beyond product-scoped filtering

### Follow-up work
- Add admin moderation list/search endpoints and UI
- Add review edit/delete or replacement-review flows
- Add spam heuristics, rate limiting, and reputation signals
- Decide whether rating aggregates should later project into catalog/search read models

## 2026-03-17 - promotion-service extraction completed

### Task
- Implemented `promotion-service` with admin CRUD, promo-code validation, deterministic cart discount evaluation, and gateway-routable promotion endpoints.

### Why
- Promotions already behaved like their own operating surface in admin and checkout flows.
- The platform needed one stable service boundary for promotion definitions and discount evaluation without committing to a full generic rule engine.

### Key files touched
- `services/promotion-service/src/main/java/com/noura/promotion/controller/PromotionController.java`
- `services/promotion-service/src/main/java/com/noura/promotion/service/impl/PromotionServiceImpl.java`
- `services/promotion-service/src/main/resources/db/migration/V1__promotion_foundation.sql`
- `apps/api-gateway/src/main/resources/application.yml`
- `docs/api/promotion-service.md`
- `docs/architecture/promotion-service.md`

### Architecture decisions
- `promotion-service` owns promotion definitions, promo/coupon identifiers, scope mappings, and deterministic discount evaluation.
- The service reuses archived promotion model and evaluation logic instead of introducing a new rule engine.
- `code` and `couponCode` are treated as one logical lookup namespace in the service layer to avoid ambiguous promo-code resolution.
- Automatic promotions remain eligible during promo-code evaluation unless blocked by `stackable=false`.

### Evaluation model
- Promotions are evaluated in descending `priority`.
- Eligibility checks run across archive state, active flag, date window, total usage limit, customer segment, scope mappings, and type-specific conditions.
- `stackable=false` short-circuits the evaluation chain after the first successful match.
- Public validation distinguishes `valid` from `eligible` and returns stable reason codes.

### Integration notes
- `apps/api-gateway` now routes `/api/v1/promotions/**` and `/api/v1/admin/promotions/**` to `promotion-service`
- `apps/admin-web` control-center endpoint catalog now exposes promotion-service endpoints explicitly
- No order/cart mutation is performed yet; this slice is read/evaluate only

### Known caveats
- `usageLimitPerCustomer` is stored but not enforced yet because there is no redemption ledger
- No event publication exists yet for promotion-applied analytics or order coordination
- Collection applicability still depends on deterministic `collectionProductIds` condition data

### Follow-up work
- Integrate `checkout-service` and `cart-service` directly with `promotion-service`
- Add redemption tracking so usage counts can move on successful order/checkout events
- Decide whether future advanced promotion logic still fits the deterministic evaluator or needs a richer policy model

## 2026-03-17 - shipping-service extraction completed

### Task
- Implemented `shipping-service` with shipping method discovery, rule-based quote calculation, shipment creation, shipment reads, internal fulfillment-status updates, and future-ready carrier adapter boundaries.

### Why
- Shipment state, tracking references, and later carrier polling/callback behavior create a cleaner operational boundary when isolated from `order-service`.
- The platform needs a stable internal shipping contract before real carrier adapters and broader fulfillment decomposition are introduced.

### Key files touched
- `services/shipping-service/src/main/java/com/noura/shipping/controller/ShippingController.java`
- `services/shipping-service/src/main/java/com/noura/shipping/service/impl/ShippingServiceImpl.java`
- `services/shipping-service/src/main/java/com/noura/shipping/provider/ShippingCarrier.java`
- `services/shipping-service/src/main/java/com/noura/shipping/provider/RuleBasedShippingCarrier.java`
- `services/shipping-service/src/main/java/com/noura/shipping/integration/client/OrderServiceClient.java`
- `services/shipping-service/src/main/java/com/noura/shipping/domain/entity/ShipmentRecord.java`
- `services/shipping-service/src/main/resources/db/migration/V1__shipping_foundation.sql`
- `docs/api/shipping-service.md`
- `docs/architecture/shipping-service.md`

### Architecture decisions
- `shipping-service` owns shipment lifecycle, tracking identifiers, external shipment references, and failure reasons.
- `order-service` remains the read-only source for order identity, order ownership, and shipment-recipient address snapshots.
- Carrier-specific behavior is hidden behind `ShippingCarrier`.
- The built-in `rule-based` carrier is deterministic and metadata-driven so storefront and orchestration flows can integrate before real carriers exist.
- Internal status updates provide the stable hook shape for warehouse events and future carrier callbacks.

### Status model
- Primary statuses:
  - `CREATED`
  - `LABEL_CREATED`
  - `READY_FOR_FULFILLMENT`
  - `IN_TRANSIT`
  - `OUT_FOR_DELIVERY`
  - `DELIVERED`
  - `EXCEPTION`
  - `RETURNED`
  - `CANCELLED`
- Terminal statuses:
  - `DELIVERED`
  - `RETURNED`
  - `CANCELLED`
- Derived fulfillment hooks map shipment states to downstream order-shipment signals without making `shipping-service` write order state directly.

### Quote model
- `standard`, `express`, and `same_day` are served through the internal `rule-based` carrier.
- Standard shipping becomes free when the subtotal reaches the configured threshold.
- Same-day availability is limited to configured cities and one configured country in this first slice.
- No currency conversion is performed yet; quotes use the request or order currency code.

### Integration notes
- `shipping-service` reads order snapshots from `order-service`
- `ORDER_SERVICE_INTERNAL_API_KEY` is supported for trusted service-to-service lookups
- One active shipment per order is enforced in the first slice; replacement shipments are allowed only after `CANCELLED` or `RETURNED`
- No order mutation or event-bus publication is performed yet

### Known caveats
- No real carrier adapter exists yet
- No shipment webhooks or scheduled polling job exist yet
- Split shipments and partial fulfillment are intentionally deferred
- Service-area ownership and external rate shopping are not implemented yet

### Follow-up work
- Add real FedEx/UPS/DHL/local courier adapters
- Add carrier webhook verification and polling jobs
- Add split-shipment and partial-fulfillment support
- Publish shipment state changes through outbox/events
- Add service-area zoning and reconciliation/reporting jobs

## 2026-03-17 - payment-service extraction completed

### Task
- Implemented `payment-service` as the first production-ready payment abstraction with internal payment intent records, pluggable provider integration, confirm flows, lookup APIs, internal status updates, and webhook-ready processing.

### Why
- Payment provider behavior and webhook retries create a cleaner operational boundary when isolated from `order-service`.
- The platform needs a stable internal payment contract before real provider adapters such as Stripe or PayPal are introduced.

### Key files touched
- `services/payment-service/src/main/java/com/noura/payment/controller/PaymentController.java`
- `services/payment-service/src/main/java/com/noura/payment/service/impl/PaymentServiceImpl.java`
- `services/payment-service/src/main/java/com/noura/payment/provider/PaymentProvider.java`
- `services/payment-service/src/main/java/com/noura/payment/provider/SandboxPaymentProvider.java`
- `services/payment-service/src/main/java/com/noura/payment/integration/client/OrderServiceClient.java`
- `services/payment-service/src/main/java/com/noura/payment/domain/entity/PaymentTransaction.java`
- `services/payment-service/src/main/java/com/noura/payment/domain/entity/PaymentWebhookEvent.java`
- `services/payment-service/src/main/resources/db/migration/V2__payment_lifecycle_and_webhooks.sql`
- `docs/api/payment-service.md`
- `docs/architecture/payment-service.md`

### Architecture decisions
- `payment-service` owns payment lifecycle, provider transaction IDs, failure reasons, and webhook deduplication.
- `order-service` remains the read-only source for order totals and customer ownership in this slice.
- Provider-specific behavior is hidden behind `PaymentProvider`.
- The built-in `mock`/`sandbox` adapter is deterministic and metadata-driven so frontend and orchestration flows can be tested before real providers exist.
- Webhook deduplication uses persisted `(provider_code, provider_event_id)` rather than in-memory replay protection.

### Status model
- Primary statuses:
  - `CREATED`
  - `REQUIRES_CONFIRMATION`
  - `PENDING`
  - `AUTHORIZED`
  - `CAPTURED`
  - `FAILED`
  - `CANCELED`
  - `REFUNDED`
- Terminal statuses:
  - `CAPTURED`
  - `FAILED`
  - `CANCELED`
  - `REFUNDED`
- Separate `authorizationStatus` and `captureStatus` fields were added for future partial capture/refund work.

### Webhook strategy
- Generic route shape: `/api/v1/payments/webhooks/{providerCode}`
- Deliveries are stored in `payment_webhook_events`
- Duplicates are ignored via unique provider event identity
- Unknown payments are recorded as failed deliveries instead of mutating any payment record
- Sandbox signature verification is a placeholder shared-secret comparison via `X-Mock-Signature`

### Integration notes
- `payment-service` reads order snapshots from `order-service`
- `ORDER_SERVICE_INTERNAL_API_KEY` is supported for trusted service-to-service lookups
- No order mutation or event-bus publication is performed yet

### Known caveats
- No real provider adapter exists yet
- No public refund API exists yet
- No outbox/event publication or reconciliation job exists yet
- Signature verification is only placeholder-grade for the sandbox provider

### Follow-up work
- Add real Stripe/PayPal adapters
- Add refund and void command APIs
- Add dispute/chargeback handling
- Publish payment state changes through outbox/events
- Add reconciliation jobs and operational reporting
