# Payment Service API

## Overview

`payment-service` owns internal payment state for existing orders. It validates orders through `order-service`, persists provider-agnostic payment records, and exposes a webhook-ready provider ingestion model.

Gateway exposure:
- `api-gateway` routes `/api/v1/payments/**` and `/api/payments/**` to `payment-service`.

Current orchestration note:
- `checkout-service` is the active synchronous caller for intent creation and confirmation in the storefront purchase flow.

All endpoints use the standard API envelope:

```json
{
  "success": true,
  "message": "Operation message",
  "data": {},
  "path": "/api/v1/..."
}
```

## Ownership and access

- `payment-service` owns payment status, provider transaction references, webhook event deduplication, and failure reasons.
- `order-service` remains the source of truth for order totals and order identity.
- `payment-service` does not mutate order state in this v1 extraction.
- Customer-facing reads and confirm flows use `X-Auth-Subject` ownership checks when available.
- Internal/admin reads are allowed through role headers or `X-Internal-Api-Key` when configured.

## Status model

Primary payment lifecycle:

- `CREATED`
- `REQUIRES_CONFIRMATION`
- `PENDING`
- `AUTHORIZED`
- `CAPTURED`
- `FAILED`
- `CANCELED`
- `REFUNDED`

Terminal states:

- `CAPTURED`
- `FAILED`
- `CANCELED`
- `REFUNDED`

Sub-state tracking:

- `authorizationStatus`: `NOT_REQUESTED | PENDING | AUTHORIZED | FAILED | CANCELED`
- `captureStatus`: `NOT_CAPTURED | PENDING | CAPTURED | FAILED | CANCELED | REFUNDED`

## Create payment intent

### `POST /api/v1/payments/intents`

Request body:

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

Behavior:

- looks up the order through `order-service`
- rejects currency mismatch against the order snapshot
- replays the existing record when `(orderId, customerRef, idempotencyKey)` already exists
- creates a provider-backed payment intent and persists the normalized internal record

Response `data` example:

```json
{
  "id": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
  "orderId": "22222222-2222-2222-2222-222222222222",
  "customerRef": "customer-2",
  "paymentReference": "pay_1b41935136a24c71a79e",
  "methodType": "CARD",
  "status": "REQUIRES_CONFIRMATION",
  "authorizationStatus": "NOT_REQUESTED",
  "captureStatus": "NOT_CAPTURED",
  "providerCode": "mock",
  "providerTransactionId": "mock_txn_1b41935136a24c71a79e",
  "amount": 49.99,
  "currencyCode": "USD",
  "autoCapture": false,
  "metadata": {
    "sandboxScenario": "pending_capture"
  }
}
```

## Confirm payment

### `POST /api/v1/payments/{paymentId}/confirm`

Request body:

```json
{
  "action": "CAPTURE"
}
```

Or authorize only:

```json
{
  "action": "AUTHORIZE"
}
```

Behavior:

- `AUTHORIZE` transitions toward `AUTHORIZED`
- `CAPTURE` transitions toward `CAPTURED`
- repeated confirm calls are safe when the payment is already pending, already authorized, or already captured
- terminal failed/canceled/refunded states reject further confirmation

## Get payment by id

### `GET /api/v1/payments/{paymentId}`

Returns the current payment record when the actor owns the underlying order or has admin/internal access.

## Get latest payment by order id

### `GET /api/v1/payments/order/{orderId}`

Returns the most recently updated payment for the order.

## Internal payment status update

### `POST /internal/payments/status-update`

Header:

- `X-Internal-Api-Key` when `APP_INTERNAL_API_KEY` is configured

Request body:

```json
{
  "paymentId": "55555555-5555-5555-5555-555555555555",
  "providerCode": "mock",
  "status": "CAPTURED",
  "providerTransactionId": "mock_txn_555",
  "providerEventId": "evt-manual-001",
  "eventType": "internal.status-update",
  "metadata": {
    "operator": "ops-console"
  }
}
```

Behavior:

- supports manual or pre-normalized upstream status changes
- persists provider event correlation when `providerEventId` is supplied
- rejects invalid lifecycle transitions

## Provider webhook

### `POST /api/v1/payments/webhooks/{providerCode}`

Current provider:

- `mock` (also accepts `sandbox`)

Sandbox payload example:

```json
{
  "eventId": "evt-2",
  "eventType": "payment.captured",
  "paymentReference": "pay_1b41935136a24c71a79e",
  "providerTransactionId": "mock_txn_1b41935136a24c71a79e"
}
```

Optional header:

- `X-Mock-Signature` when `PAYMENT_SANDBOX_WEBHOOK_SECRET` is configured

Behavior:

- parses raw provider payload through the provider adapter
- stores the delivery in `payment_webhook_events`
- deduplicates by `(provider_code, provider_event_id)`
- records unmatched deliveries as failed webhook events without mutating payment state
- returns `202 Accepted` for processed, ignored, duplicate, or unmatched deliveries

Webhook response `data` example:

```json
{
  "providerCode": "mock",
  "providerEventId": "evt-2",
  "eventType": "payment.captured",
  "processingStatus": "PROCESSED",
  "duplicate": false,
  "signatureVerified": false,
  "paymentId": "55555555-5555-5555-5555-555555555555",
  "paymentStatus": "CAPTURED",
  "message": "Webhook processed"
}
```

## Sandbox scenarios

The built-in sandbox adapter reads `metadata.sandboxScenario` from the payment record:

- `fail_create`
- `fail_authorize`
- `pending_authorize`
- `fail_capture`
- `pending_capture`

This keeps frontend, checkout, and internal integration work moving before a real provider is wired.

## Known limitations

- No real provider adapter is configured yet.
- No refund command API is exposed yet; `REFUNDED` is currently available through internal updates or webhook flows.
- Signature verification is a placeholder shared-secret check for the sandbox provider only.
- No outbox/event-bus publication is implemented yet.
