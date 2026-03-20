# Services

Canonical service targets:

- `catalog-service`
- `search-service`
- `pricing-service`
- `cart-service`
- `checkout-service`
- `order-service`
- `payment-service`
- `inventory-service`
- `customer-service`
- `promotion-service`
- `shipping-service`
- `notification-service`
- `review-service`

Current state:
- `notification-service` is extracted and runnable.
- `catalog-service` is extracted for read APIs and routes through `api-gateway`.
- `catalog-service` also carries transitional admin recommendation/merchandising compatibility endpoints to keep current admin-web screens working without a separate legacy app-service.
- `search-service` is extracted for projection-backed discovery APIs, internal indexing contracts, and future search-provider abstraction while `catalog-service` keeps browse/admin product search.
- `inventory-service` is extracted for stock visibility and stock mutation APIs.
- `pricing-service` is extracted for product price upsert and storefront/checkout price resolution.
- `cart-service` is extracted for persistent carts with catalog/pricing/inventory validation plus promotion-service coupon validation integration.
- `customer-service` is extracted for storefront customer profile/address flows.
- `order-service` is extracted for order aggregate creation/history/status.
- `checkout-service` is extracted for preview/validation/place-order orchestration, synchronous payment confirmation, internal order finalization, and best-effort notification triggering.
- `promotion-service` is extracted for deterministic promotion CRUD, promo-code validation, and cart discount evaluation.
- `payment-service` is extracted for internal payment intent lifecycle, sandbox provider orchestration, and webhook-ready status handling.
- `shipping-service` is extracted for rule-based shipping method discovery, shipment lifecycle records, and carrier-ready status hooks.
- `shipping-service` also owns the extracted merchant/store/service-area compatibility slice currently used by admin-web store-ops pages.
- `review-service` is extracted for moderated storefront product reviews, approved-only rating aggregation, and admin moderation actions.
- Other services are extraction targets reusing logic from `archive/legacy-monolith/backend-monolith`.

## Migration discipline

- Java baseline:
  - all extracted services and `apps/api-gateway` now compile and run against `JDK 25`
  - Docker build/runtime images are aligned to Temurin 25 so local Docker builds match host-based Maven runs
- Service-owned persistence uses Flyway with one schema-history table per service.
- Transactional services should keep `spring.jpa.hibernate.ddl-auto=validate` so entity drift fails fast against migrated schemas.
- Cross-service foreign keys are intentionally avoided; only intra-service ownership relations should use database FKs.
- `catalog-service` is the explicit exception in this repo today: it is read-only over shared catalog tables and does not own a Flyway migration set yet.

Run one service migration stack locally with:

```bash
cd services/<service-name>
mvn spring-boot:run
```

For the full extracted platform, prefer:

```bash
./platform/scripts/run-local.sh
```

Flyway runs automatically on startup for service-owned schemas. For the current persistence standards and read-only exceptions, see [docs/database/service-persistence-standards.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/database/service-persistence-standards.md).

## Test coverage

- Commerce-critical service coverage currently prioritizes pricing lookup, promo validation, cart mutation, inventory reservation/release/deduction, checkout validation/place-order, order lifecycle updates, and payment confirm/webhook behavior.
- The preferred pattern is service-level unit tests plus Web MVC slices. Checkout additionally uses a controller-plus-service slice so the HTTP contract and orchestration behavior are validated together without a full multi-service runtime.
- Targeted test guidance and commands live in [docs/testing/commerce-service-tests.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/testing/commerce-service-tests.md).

## API consistency

- Extracted services share one API envelope shape for success and failure responses.
- Validation, not-found, auth, conflict, business-rule, and internal-error categories now follow one documented response policy instead of service-specific top-level failure wording.
- The standard and example payloads live in [docs/api/error-response-standard.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/api/error-response-standard.md).

## Operational readiness

- Every extracted service now exposes:
  - `/actuator/health`
  - `/actuator/health/readiness`
  - `/actuator/health/liveness`
- Every extracted service propagates `X-Correlation-ID` and emits one structured access log line per request.
- Every service logs one `service_startup` summary on boot with port, probe paths, and active profiles.
- Docker images now use a liveness `HEALTHCHECK` against `/actuator/health/liveness`.
- The shared local/dev runbook lives in [docs/operations/local-service-readiness.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/operations/local-service-readiness.md).
