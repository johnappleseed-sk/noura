# Shipping Service Architecture

## Purpose

`shipping-service` is the extracted shipping and shipment boundary for NOURA. It provides carrier-agnostic shipping method discovery, quote calculation, and shipment lifecycle records without pushing carrier-specific concepts into `order-service`.

## Ownership boundary

`shipping-service` owns:

- shipping method resolution
- quote calculation results
- shipment record identity
- carrier shipment IDs and tracking numbers
- shipment lifecycle state
- failure reasons
- fulfillment-hook hints for downstream order state propagation

`order-service` owns:

- order identity
- customer ownership
- order lifecycle state
- shipping address snapshot used as the shipment recipient source in v1

Integration rule:

- `shipping-service` reads order data from `order-service`
- `shipping-service` does not write order state in v1

## Runtime shape

Main modules:

- controller layer for method discovery, quote, create, read, and internal status-update endpoints
- `ShippingServiceImpl` for lifecycle and access rules
- `OrderServiceClient` for order validation and recipient-address sourcing
- `ShippingCarrier` abstraction plus `RuleBasedShippingCarrier`
- JPA repository for `shipment_records`

## Status decisions

Primary lifecycle:

- `CREATED`
- `LABEL_CREATED`
- `READY_FOR_FULFILLMENT`
- `IN_TRANSIT`
- `OUT_FOR_DELIVERY`
- `DELIVERED`
- `EXCEPTION`
- `RETURNED`
- `CANCELLED`

Allowed transitions:

- `CREATED -> LABEL_CREATED | READY_FOR_FULFILLMENT | IN_TRANSIT | EXCEPTION | CANCELLED`
- `LABEL_CREATED -> READY_FOR_FULFILLMENT | IN_TRANSIT | OUT_FOR_DELIVERY | DELIVERED | EXCEPTION | CANCELLED`
- `READY_FOR_FULFILLMENT -> IN_TRANSIT | OUT_FOR_DELIVERY | DELIVERED | EXCEPTION | CANCELLED`
- `IN_TRANSIT -> OUT_FOR_DELIVERY | DELIVERED | RETURNED | EXCEPTION`
- `OUT_FOR_DELIVERY -> DELIVERED | RETURNED | EXCEPTION`
- `EXCEPTION -> READY_FOR_FULFILLMENT | IN_TRANSIT | OUT_FOR_DELIVERY | DELIVERED | RETURNED | CANCELLED`
- `DELIVERED -> RETURNED`

Terminal states:

- `DELIVERED`
- `RETURNED`
- `CANCELLED`

Derived fulfillment hooks keep later order-state propagation simple:

- packed-ready states map to `ORDER_PACKED`
- in-flight states map to `ORDER_SHIPPED`
- delivered maps to `ORDER_DELIVERED`
- failure/cancel/return states map to operational shipment hooks

## Quote calculation model

The built-in rule-based carrier intentionally uses transparent rules:

- `standard`
  - free above a configurable subtotal threshold
  - base amount plus per-kilogram surcharge after `1kg`
- `express`
  - faster transit
  - higher surcharge after `0.5kg`
- `same_day`
  - constrained to configured cities and one configured country
  - higher surcharge and smaller weight limit

This is intentionally a business-rule engine, not a fake third-party HTTP integration. Real carriers will later implement the same carrier boundary.

## Idempotency and shipment-count model

Create shipment:

- keyed by `(orderId, customerRef, idempotencyKey)`
- replays the existing shipment record when the same command arrives again

First-slice shipment-count decision:

- one active shipment per order
- replacement shipments are allowed only after `CANCELLED` or `RETURNED`
- split shipments are deferred until order allocation and partial-fulfillment rules are designed

## Carrier abstraction boundary

Stored internally:

- `carrierCode`
- `externalShipmentId`
- `trackingNumber`
- normalized shipment status
- normalized failure reason

Hidden from `order-service`:

- carrier-specific rate logic
- carrier API payload shapes
- carrier-specific polling or callback rules
- tracking-status mapping details

This keeps the eventual logistics provider swap isolated behind one interface.

## Carrier refresh and hook strategy

Current carrier refresh:

- `GET` shipment endpoints support `refreshCarrier=true`
- the built-in carrier refresh is metadata-driven rather than time-driven
- internal status-update endpoint provides a stable ingress for warehouse events and future carrier callbacks

Planned evolution:

1. carrier webhooks or scheduled polling
2. normalized shipment status events or outbox publication
3. order-service subscription or orchestration-layer state updates

## Reuse notes

This extraction reuses and modernizes:

- legacy fulfillment carrier abstraction ideas from `archive/legacy-monolith/backend-monolith/src/main/java/com/noura/platform/commerce/fulfillment/application/ShippingCarrier.java`
- stub carrier behavior from the archived monolith
- shipment-record concepts from legacy fulfillment/order modules
- internal API and request-context patterns from extracted `payment-service` and `order-service`

## Follow-up work

- real external carrier adapters
- carrier webhook verification and normalized callback ingestion
- split-shipment and partial-fulfillment support
- service-area and delivery-zone ownership models
- label artifact storage and printable label workflows
- outbox/event publication for order-state propagation
- refund/return coordination with payment and returns flows
