# payment-service

Production payment abstraction service for internal payment intent records, provider confirmation, and webhook-ready status updates.

## Exposed endpoints

- `POST /api/v1/payments/intents` (legacy alias: `/api/payments/intents`)
- `POST /api/v1/payments/{paymentId}/confirm` (legacy alias: `/api/payments/{paymentId}/confirm`)
- `GET /api/v1/payments/{paymentId}` (legacy alias: `/api/payments/{paymentId}`)
- `GET /api/v1/payments/order/{orderId}` (legacy alias: `/api/payments/order/{orderId}`)
- `POST /api/v1/payments/webhooks/{providerCode}` (legacy alias: `/api/payments/webhooks/{providerCode}`)
- `POST /internal/payments/status-update`

## Scope (v1)

- Creates and persists payment intent records with immutable order amount and currency snapshots from `order-service`.
- Supports authorize-only and authorize+capture confirmation flows through a pluggable provider interface.
- Tracks payment lifecycle plus authorization/capture sub-state for future partial capture and refund extensions.
- Supports internal status updates and webhook-ready provider deliveries with persisted event deduplication.
- Uses sandbox/mock provider by default for local/startup execution.

## Integration behavior

- Validates order existence and ownership by querying `order-service`.
- Does not mutate order lifecycle state in this slice; order/payment coupling remains read-only.
- Keeps provider abstraction ready for future real gateway adapters such as Stripe or PayPal.
- Uses `order-service` internal API access when `ORDER_SERVICE_INTERNAL_API_KEY` is configured, while still supporting storefront-owner access via forwarded customer identity.

## Payment lifecycle

- `CREATED`: internal record initialized before provider result normalization.
- `REQUIRES_CONFIRMATION`: payment intent is ready for authorize/capture.
- `PENDING`: provider is still resolving an async authorization or capture.
- `AUTHORIZED`: funds are authorized but not yet captured.
- `CAPTURED`: payment succeeded and funds are captured.
- `FAILED`: provider declined or processing failed.
- `CANCELED`: authorization/capture was canceled or voided.
- `REFUNDED`: captured payment was refunded.

Terminal states:

- `CAPTURED`
- `FAILED`
- `CANCELED`
- `REFUNDED`

Authorization and capture sub-state are tracked separately:

- authorization: `NOT_REQUESTED | PENDING | AUTHORIZED | FAILED | CANCELED`
- capture: `NOT_CAPTURED | PENDING | CAPTURED | FAILED | CANCELED | REFUNDED`

## Sandbox provider behavior

Default provider code: `mock`

Optional metadata key `sandboxScenario` drives deterministic provider behavior:

- `fail_create`
- `fail_authorize`
- `pending_authorize`
- `fail_capture`
- `pending_capture`

Example create payload:

```json
{
  "orderId": "22222222-2222-2222-2222-222222222222",
  "methodType": "CARD",
  "providerCode": "mock",
  "currencyCode": "USD",
  "autoCapture": false,
  "idempotencyKey": "pay-intent-001",
  "metadata": {
    "sandboxScenario": "pending_capture"
  }
}
```

## Webhook behavior

- Webhook route shape is provider-generic: `/api/v1/payments/webhooks/{providerCode}`.
- Incoming provider deliveries are normalized and stored in `payment_webhook_events`.
- Duplicate deliveries are deduplicated by `(provider_code, provider_event_id)`.
- When the sandbox webhook secret is configured, `X-Mock-Signature` must match it.
- Unknown payments are recorded as failed webhook deliveries without mutating payment state.

## Local run

```bash
cd services/payment-service
mvn spring-boot:run
```

Required environment variables:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

Optional environment variables:

- `SERVER_PORT` (default `8080`)
- `ORDER_SERVICE_BASE_URL` (default `http://localhost:8090`)
- `APP_INTERNAL_API_KEY`
- `ORDER_SERVICE_INTERNAL_API_KEY`
- `PAYMENT_SANDBOX_WEBHOOK_SECRET`

## Persistence notes

- Flyway migrations:
  - `V1__payment_foundation.sql`
  - `V2__payment_lifecycle_and_webhooks.sql`
  - `V3__payment_persistence_cleanup.sql`
- Payment enums are stored as strings, not ordinals.
- The persistence cleanup migration adds composite indexes for `order -> latest payment` lookups and check constraints for non-negative component amounts.
