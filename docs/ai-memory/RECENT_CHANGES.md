# Recent Changes

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
