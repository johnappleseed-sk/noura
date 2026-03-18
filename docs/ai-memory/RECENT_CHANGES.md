# Recent Changes

## 2026-03-18 - pragmatic automated commerce test coverage

### Task
- Added targeted automated coverage for the highest-risk commerce flows across pricing, promotion, cart, inventory, checkout, order, and payment services.

### Why
- The extracted services already had strong coverage in some areas, but core mutation and happy-path workflow gaps remained around cart updates, stock release, checkout HTTP orchestration, successful order status history, and payment confirmation.
- The goal was to raise confidence in the current commerce platform without introducing a brittle or overbuilt full environment test harness.

### Key files touched
- `services/cart-service/src/test/java/com/noura/cart/service/CartServiceImplTest.java`
- `services/inventory-service/src/test/java/com/noura/inventory/service/InventoryStockServiceImplTest.java`
- `services/inventory-service/src/test/java/com/noura/inventory/controller/InventoryStockControllerTest.java`
- `services/checkout-service/src/test/java/com/noura/checkout/controller/CheckoutControllerIntegrationTest.java`
- `services/order-service/src/test/java/com/noura/order/service/impl/OrderServiceImplTest.java`
- `services/payment-service/src/test/java/com/noura/payment/service/impl/PaymentServiceImplTest.java`
- `docs/testing/commerce-service-tests.md`

### Architecture decisions
- Kept the test strategy pragmatic: unit tests for business rules and Web MVC slices for HTTP contracts.
- Used a controller-plus-service checkout test instead of a full multi-service runtime test so the request contract and orchestration logic are both exercised without introducing infrastructure-heavy fragility.
- Did not add repository-specific slice tests in this pass because the highest signal remained in domain/service behavior and HTTP contract coverage.

### Integration notes
- Commerce-critical scenarios now covered directly by automated tests include:
  - price lookup
  - promo validation/evaluation
  - add/update/remove cart item
  - stock reserve/release/deduct guardrails
  - checkout validation
  - checkout place-order happy path
  - order creation status history
  - order status update
  - payment confirm/capture
  - low-stock lookup
- Checkout happy-path coverage uses the real controller and orchestration service with mocked downstream service clients.

### Known caveats
- There is still no containerized gateway-to-services end-to-end suite.
- Search, shipping, review, and customer-service test expansion were not the focus of this pass because the requested critical commerce scenarios were already better represented elsewhere or fell outside the highest-risk path.
- Repository-query behavior is still validated indirectly through service tests rather than dedicated `@DataJpaTest` coverage.

### Follow-up work
- Add gateway-level or docker-compose smoke tests for the full purchase flow once the local platform bootstrap is stable enough for CI.
- Expand shipping and notification test coverage when shipment orchestration becomes part of checkout.
- Revisit repository-slice tests if service query complexity increases beyond the current simple lookup patterns.

## 2026-03-18 - database and migration cleanup across extracted services

### Task
- Audited extracted service persistence and cleaned up migration gaps, lookup indexes, audit timestamp defaults, and intra-service relational constraints.

### Why
- Several extracted services had drift between runtime JPA entities and Flyway-owned tables.
- Some first-wave migrations relied on application callbacks for audit timestamps instead of also setting database defaults.
- A few hot lookup paths used in checkout, payments, notifications, reviews, and stock history did not yet have explicit supporting indexes.

### Key files touched
- `services/customer-service/src/main/resources/db/migration/V2__customer_payment_methods.sql`
- `services/pricing-service/src/main/resources/db/migration/V2__pricing_admin_compatibility.sql`
- `services/inventory-service/src/main/resources/db/migration/V2__inventory_lookup_indexes.sql`
- `services/notification-service/src/main/resources/db/migration/V2__notification_audit_cleanup.sql`
- `services/promotion-service/src/main/resources/db/migration/V2__promotion_persistence_cleanup.sql`
- `services/review-service/src/main/resources/db/migration/V2__review_persistence_cleanup.sql`
- `services/payment-service/src/main/resources/db/migration/V3__payment_persistence_cleanup.sql`
- `services/shipping-service/src/main/resources/db/migration/V3__shipping_persistence_cleanup.sql`
- `docs/database/service-persistence-standards.md`

### Architecture decisions
- `customer-service` now owns its saved payment-method table through Flyway instead of relying on an unmigrated runtime entity.
- `pricing-service` now owns the lightweight `legacy_price_lists` compatibility table through Flyway so admin-web pricing pages no longer depend on an implicit schema.
- Extracted services continue to standardize on string-backed enums and database-managed `created_at` / `updated_at` columns.
- The platform still does not use JPA optimistic locking; current concurrency control remains idempotency + targeted pessimistic locks.
- `catalog-service` remains the deliberate read-only exception until catalog write ownership moves into a service-owned schema plan.

### Integration notes
- Added lookup indexes for:
  - inventory stock-level product timelines
  - inventory stock-movement history
  - notification unread-count queries
  - payment latest-by-order lookups
  - review product timelines
  - shipping case-insensitive merchant/store code lookups
- Added the missing `store_records.merchant_id -> merchant_records.id` foreign key because both tables are owned by `shipping-service`.

### Known caveats
- `catalog-service`, `search-service`, and some search-side read models still depend on shared canonical tables outside service-owned Flyway control.
- Notification persistence still standardizes timestamp audit only; it does not yet have a stable actor audit contract comparable to customer/order/payment/shipping writes.
- Older migrations are still heterogeneous in style (`IF NOT EXISTS` coverage, inline checks, default clauses), but the current standards are now documented for future work.

### Follow-up work
- Decide when catalog write ownership is ready for a service-owned migration set instead of shared read-only table mappings.
- Consider adding dedicated migration validation/integration tests against PostgreSQL or Testcontainers so future schema drift is caught before runtime.
- Revisit search/catalog shared read-only table dependencies once projection rebuilds are fully event-driven.

## 2026-03-18 - frontend/backend contract alignment pass completed

### Task
- Aligned extracted backend APIs with the currently implemented `apps/storefront-web` and `apps/admin-web` contract surface.

### Why
- Frontend apps still depended on several legacy monolith paths that no longer had an extracted backend owner.
- Storefront recommendation, checkout-step, account payment-method, and quick-reorder flows needed compatibility endpoints to avoid frontend rework.
- Admin recommendation, merchandising, merchant, store, and service-area pages still assumed controllers that existed only in the archived monolith.

### Key files touched
- `services/catalog-service/src/main/java/com/noura/catalog/controller/CatalogPublicController.java`
- `services/catalog-service/src/main/java/com/noura/catalog/controller/CatalogRecommendationController.java`
- `services/catalog-service/src/main/java/com/noura/catalog/controller/CatalogAdminCompatibilityController.java`
- `services/catalog-service/src/main/java/com/noura/catalog/service/impl/CatalogAdminCompatibilityServiceImpl.java`
- `services/checkout-service/src/main/java/com/noura/checkout/controller/CheckoutController.java`
- `services/customer-service/src/main/java/com/noura/customer/controller/CustomerAccountController.java`
- `services/order-service/src/main/java/com/noura/order/controller/OrderController.java`
- `services/pricing-service/src/main/java/com/noura/pricing/controller/PricingLegacyCompatibilityController.java`
- `services/shipping-service/src/main/java/com/noura/shipping/controller/FulfillmentNetworkController.java`
- `services/shipping-service/src/main/java/com/noura/shipping/service/impl/FulfillmentNetworkServiceImpl.java`
- `apps/api-gateway/src/main/resources/application.yml`

### Archive reuse decisions
- Reused archived recommendation, merchandising, merchant, store, and service-area controller shapes as the compatibility contract reference.
- Reused archived DTO field names where that reduced frontend churn directly.
- Did not revive archived `app-service`-style catch-all ownership; each migrated contract was assigned to an extracted service with a concrete bounded-context rationale.

### Architecture decisions
- Catalog-service now owns transitional admin recommendation/merchandising control compatibility because those pages operate over catalog-derived ranking and preview data.
- Shipping-service now owns the extracted merchant/store/service-area compatibility slice because active store coverage belongs closest to fulfillment and shipping decisions.
- Legacy pricing, checkout-step, payment-method, quick-reorder, and storefront recommendation/product-rail contracts were preserved as compatibility adapters instead of rewriting frontend clients.
- Gateway remains the canonical ingress and now routes the new admin compatibility endpoints to extracted services rather than the legacy `app-service`.

### Integration notes
- New extracted admin compatibility routes now exist for:
  - recommendation settings and preview
  - merchandising settings, boosts, and preview
  - merchants
  - stores and store-location updates
  - service areas and validation sandbox
- Storefront compatibility routes now cover:
  - merchandising product listing
  - recommendations rails plus `mock-ai`
  - related products and frequently-bought-together
  - checkout step APIs
  - account payment methods
  - quick reorder

### Known caveats
- Catalog admin recommendation/merchandising control state is a lightweight transitional compatibility layer, not yet a dedicated long-term control-plane service.
- `location`, `admin/carousels`, `admin/product-submissions`, and `admin/recovery` still assume legacy-only ownership and remain unresolved.
- Storefront build still logs `ECONNREFUSED` during static prerender when local APIs are not running, but the build completes successfully.

### Follow-up work
- Extract an explicit owner for `location` APIs instead of leaving them on the dead legacy route.
- Decide whether carousels/product submissions/recovery belong in a control-plane service or a modular governance service.
- Replace lightweight catalog admin control state with a persistent extracted admin configuration owner once the control plane is split out.

## 2026-03-18 - end-to-end purchase flow completed

### Task
- Completed one executable happy-path purchase flow across storefront, checkout-service, order-service, payment-service, customer-service, inventory-service, cart-service, and notification-service.

### Why
- Checkout still stopped before real payment confirmation and order finalization.
- Storefront direct checkout did not send the `storeId` or typed payment fields needed by the backend contract.
- Notification dispatch could not target the correct user because checkout only held the external customer subject.

### Key files touched
- `services/checkout-service/src/main/java/com/noura/checkout/service/impl/CheckoutOrchestrationServiceImpl.java`
- `services/checkout-service/src/main/java/com/noura/checkout/service/impl/PaymentServiceGateway.java`
- `services/checkout-service/src/main/java/com/noura/checkout/integration/client/OrderServiceClient.java`
- `services/checkout-service/src/main/java/com/noura/checkout/integration/client/CustomerServiceClient.java`
- `services/checkout-service/src/main/java/com/noura/checkout/integration/client/NotificationServiceClient.java`
- `services/checkout-service/src/main/java/com/noura/checkout/controller/CheckoutController.java`
- `services/order-service/src/main/java/com/noura/order/controller/InternalOrderLifecycleController.java`
- `apps/storefront-web/lib/api.js`
- `apps/storefront-web/app/cart/page.jsx`
- `docs/architecture/purchase-flow.md`

### Architecture decisions
- Checkout remains synchronous for the first complete purchase flow.
- `payment-service` still owns provider state and confirmation, while `checkout-service` owns the orchestration that turns successful payment into a finalized order.
- Internal order finalization uses a narrow trusted endpoint instead of reusing the public admin order-status route.
- Notification dispatch now resolves the internal customer UUID through `customer-service` before calling `notification-service`.
- Payment success for checkout finalization currently means `AUTHORIZED` or `CAPTURED`.

### Integration notes
- Storefront direct checkout now sends:
  - `storeId`
  - `paymentMethod`
  - `paymentProvider`
  - `paymentProviderReference`
  - `paymentAutoCapture`
  - `idempotencyKey`
- Checkout derives a payment-scoped idempotency key from the checkout key.
- Successful checkout now returns nested `order` and `payment` summaries.
- Payment failures release reservations and attempt to cancel the created order.

### Known caveats
- Shipping orchestration is still not triggered automatically during checkout placement.
- Order finalization currently updates status only; order-service does not persist the payment reference written back from checkout.
- Status spelling is still domain-specific (`CANCELED` in payment vs `CANCELLED` in order/shipping); mapping harmonization is pending.
- Storefront build still logs `ECONNREFUSED` during static prerender when local APIs are not running, but the build completes successfully.

### Follow-up work
- Add shipment creation/finalization into the checkout happy path once shipping ownership is finalized.
- Decide whether order-service should persist payment reference updates from checkout finalization.
- Add gateway-level or end-to-end integration tests for the full purchase path.
- Add compensation/outbox handling for post-payment order finalization failures.

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
