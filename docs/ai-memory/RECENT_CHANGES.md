# Recent Changes

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
