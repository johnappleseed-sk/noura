# Payment Service Architecture

## Purpose

`payment-service` is the extracted payment boundary for NOURA. It provides a provider-agnostic internal payment record, confirmation lifecycle, and webhook ingestion model without pushing provider-specific details into `order-service`.

## Ownership boundary

`payment-service` owns:

- payment intent identity
- provider transaction references
- payment lifecycle state
- authorization/capture sub-state
- failure reasons
- webhook delivery deduplication

`order-service` owns:

- order identity
- immutable order total and currency snapshot
- order lifecycle state

Integration rule:

- `payment-service` reads order data from `order-service`
- `payment-service` does not write order state in v1

## Runtime shape

Main modules:

- controller layer for create/confirm/read/internal/webhook endpoints
- `PaymentServiceImpl` for lifecycle and access rules
- `OrderServiceClient` for order validation
- `PaymentProvider` abstraction plus `SandboxPaymentProvider`
- JPA repositories for `payment_transactions` and `payment_webhook_events`

## Status decisions

Primary lifecycle:

- `CREATED`
- `REQUIRES_CONFIRMATION`
- `PENDING`
- `AUTHORIZED`
- `CAPTURED`
- `FAILED`
- `CANCELED`
- `REFUNDED`

Allowed transitions:

- `CREATED -> REQUIRES_CONFIRMATION | PENDING | FAILED | CANCELED`
- `REQUIRES_CONFIRMATION -> PENDING | AUTHORIZED | CAPTURED | FAILED | CANCELED`
- `PENDING -> AUTHORIZED | CAPTURED | FAILED | CANCELED`
- `AUTHORIZED -> CAPTURED | CANCELED`
- `CAPTURED -> REFUNDED`

Terminal states:

- `CAPTURED`
- `FAILED`
- `CANCELED`
- `REFUNDED`

Separate sub-state fields keep future partial-capture and refund extensions feasible:

- `authorizationStatus`
- `captureStatus`
- `authorizedAmount`
- `capturedAmount`
- `refundedAmount`

## Authorization and capture model

- Intent creation snapshots the order and provider selection, then lands in `REQUIRES_CONFIRMATION`.
- Confirm with `AUTHORIZE` performs auth-only processing.
- Confirm with `CAPTURE` performs capture semantics and allows the provider adapter to implement auth-and-capture in one step.
- The sandbox adapter supports deterministic pending and failure simulations without external systems.

This keeps the public API simple while leaving room for future separate capture/refund commands.

## Idempotency model

Create intent:

- keyed by `(orderId, customerRef, idempotencyKey)`
- replays the existing payment record instead of reissuing provider creation

Confirm:

- safe for repeated calls when payment is already pending or already in the requested satisfied state
- terminal states reject further confirmation

Webhook:

- every delivery is recorded in `payment_webhook_events`
- `(provider_code, provider_event_id)` is the webhook deduplication key
- unmatched events are stored as failed deliveries for traceability

## Webhook strategy

Provider adapters normalize raw webhook payloads into:

- internal payment status
- authorization/capture sub-state
- provider transaction ID
- provider event identity
- signature verification result

Processing flow:

1. Parse and validate provider payload.
2. Persist the webhook delivery as `RECEIVED`.
3. Resolve the payment by `paymentReference` or `providerTransactionId`.
4. Apply lifecycle transition rules.
5. Mark the delivery as `PROCESSED`, `IGNORED`, or `FAILED`.

## Provider abstraction boundary

Stored internally:

- `providerCode`
- `providerTransactionId`
- normalized payment status
- normalized failure reason
- webhook event identity

Hidden from `order-service`:

- provider-specific payload shapes
- webhook headers/signatures
- provider state machine quirks

This keeps `order-service` insulated from vendor-specific behavior and lets future providers swap in behind one boundary.

## Reuse notes

This extraction reuses and modernizes:

- legacy payment gateway abstraction ideas from `archive/legacy-monolith/backend-monolith/src/main/java/com/noura/platform/commerce/payments/application/PaymentGateway.java`
- stub/mock provider patterns from the archived monolith
- internal API and request-context patterns from extracted `order-service`, `customer-service`, and `checkout-service`

## Follow-up work

- real Stripe/PayPal provider adapters
- refund command API
- dispute/chargeback handling
- stronger signature verification per real provider
- outbox/event publication for payment state changes
- reconciliation jobs and operational dashboards
