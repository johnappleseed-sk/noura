# Shipping Service API

## Overview

`shipping-service` owns shipping method discovery, rule-based quote calculation, shipment lifecycle records, and fulfillment-ready shipment status hooks. It validates orders through `order-service`, persists carrier-agnostic shipment records, and keeps the carrier boundary ready for future external logistics integrations.

Gateway exposure:
- `api-gateway` routes `/api/v1/shipping/**` and `/api/shipping/**` to `shipping-service`.

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

- `shipping-service` owns shipment records, tracking identifiers, shipment status, failure reasons, and carrier refresh logic.
- `order-service` remains the source of truth for order identity, customer ownership, and the shipping address snapshot.
- `shipping-service` does not mutate order state in this v1 extraction.
- Customer-facing shipment reads and creation flows use `X-Auth-Subject` ownership checks when available.
- Internal/admin reads and status updates are allowed through role headers or `X-Internal-Api-Key` when configured.

## Shipment status model

Shipment lifecycle:

- `CREATED`
- `LABEL_CREATED`
- `READY_FOR_FULFILLMENT`
- `IN_TRANSIT`
- `OUT_FOR_DELIVERY`
- `DELIVERED`
- `EXCEPTION`
- `RETURNED`
- `CANCELLED`

Terminal states:

- `DELIVERED`
- `RETURNED`
- `CANCELLED`

Derived fulfillment hooks:

- `ORDER_PACKED`
- `ORDER_SHIPPED`
- `ORDER_DELIVERED`
- `SHIPMENT_EXCEPTION`
- `SHIPMENT_CANCELLED`
- `SHIPMENT_RETURNED`

## Quote model

Current built-in carrier:

- `rule-based`

Supported methods:

- `standard`
  - free when subtotal reaches the configured threshold
  - available up to `30kg`
- `express`
  - faster transit with higher weight surcharge
  - available up to `20kg`
- `same_day`
  - available only for configured cities in the configured country
  - available up to `10kg`

No foreign-exchange conversion is performed in this first slice. Quotes are emitted in the request/order currency code.

## Get shipping methods

### `GET /api/v1/shipping/methods`

Query parameters:

- `countryCode` required
- `city` optional
- `stateProvince` optional
- `postalCode` optional
- `cartSubtotal` required
- `currencyCode` required
- `itemCount` required
- `totalWeightKg` required
- `carrierCode` optional

Example:

```http
GET /api/v1/shipping/methods?countryCode=KH&city=Phnom%20Penh&postalCode=12000&cartSubtotal=49.99&currencyCode=USD&itemCount=2&totalWeightKg=1.5
```

Behavior:

- resolves available methods through the carrier abstraction
- returns an empty list when no methods are available

Response `data` example:

```json
[
  {
    "carrierCode": "rule-based",
    "methodCode": "standard",
    "methodName": "Standard Shipping",
    "amount": 5.62,
    "currencyCode": "USD",
    "estimatedDaysMin": 3,
    "estimatedDaysMax": 5,
    "estimatedDeliveryAt": "2026-03-22T10:00:00Z",
    "supportsTracking": true,
    "ruleSummary": "Base standard shipping plus weight surcharge after the first 1.0kg"
  }
]
```

## Create shipping quote

### `POST /api/v1/shipping/quotes`

Request body:

```json
{
  "address": {
    "fullName": "Customer Example",
    "phone": "012345678",
    "line1": "Street 1",
    "city": "Phnom Penh",
    "postalCode": "12000",
    "countryCode": "KH"
  },
  "cartSubtotal": 49.99,
  "currencyCode": "USD",
  "itemCount": 2,
  "totalWeightKg": 1.5,
  "carrierCode": "rule-based",
  "methodCode": "express"
}
```

Behavior:

- selects one carrier adapter
- validates the chosen method against destination and weight rules
- returns a deterministic quote or rejects the request with `SHIPPING_METHOD_UNAVAILABLE`

## Create shipment

### `POST /api/v1/shipping/shipments`

Request body:

```json
{
  "orderId": "22222222-2222-2222-2222-222222222222",
  "carrierCode": "rule-based",
  "methodCode": "express",
  "idempotencyKey": "shipment-001",
  "signatureRequired": false,
  "parcels": [
    {
      "quantity": 2,
      "weightKg": 0.75
    }
  ],
  "metadata": {
    "shippingScenario": "ready"
  }
}
```

Behavior:

- looks up the order through `order-service`
- rejects orders that do not have a structured shipping address
- replays the existing record when `(orderId, customerRef, idempotencyKey)` already exists
- persists one shipment record per order in the current slice unless the previous shipment is `CANCELLED` or `RETURNED`
- calls the carrier adapter to create tracking data and initial shipment state

Response `data` example:

```json
{
  "id": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
  "orderId": "22222222-2222-2222-2222-222222222222",
  "orderNumber": "ORD-20260317-ABC12345",
  "customerRef": "customer-2",
  "shipmentReference": "SHP-20260317-0670406E",
  "carrierCode": "rule-based",
  "methodCode": "express",
  "methodName": "Express Shipping",
  "status": "LABEL_CREATED",
  "fulfillmentHook": "ORDER_PACKED",
  "quotedAmount": 9.99,
  "currencyCode": "USD",
  "externalShipmentId": "ship_external_123",
  "trackingNumber": "NRA123456789012",
  "trackingUrl": "https://tracking.noura.local/track/NRA123456789012"
}
```

## Get shipment by id

### `GET /api/v1/shipping/shipments/{shipmentId}`

Query parameters:

- `refreshCarrier` optional boolean, default `false`

Behavior:

- returns the current shipment record
- when `refreshCarrier=true`, calls the carrier adapter before responding

## Get latest shipment by order id

### `GET /api/v1/shipping/shipments/order/{orderId}`

Query parameters:

- `refreshCarrier` optional boolean, default `false`

Behavior:

- returns the latest shipment record for the order
- useful for order-detail and shipment-status surfaces

## Internal shipment status update

### `POST /internal/shipping/shipments/status-update`

Header:

- `X-Internal-Api-Key` when `APP_INTERNAL_API_KEY` is configured

Request body:

```json
{
  "shipmentId": "33333333-3333-3333-3333-333333333333",
  "status": "IN_TRANSIT",
  "trackingNumber": "NRA123456789012",
  "trackingUrl": "https://tracking.noura.local/track/NRA123456789012",
  "estimatedDeliveryAt": "2026-03-19T09:00:00Z",
  "source": "ops-console",
  "metadata": {
    "operator": "warehouse-01"
  }
}
```

Behavior:

- supports manual or pre-normalized upstream shipment-state changes
- merges additional metadata into the stored shipment metadata
- rejects invalid lifecycle transitions

## Idempotency and first-slice constraints

Create shipment:

- keyed by `(orderId, customerRef, idempotencyKey)`
- replays the existing shipment record instead of creating a duplicate

First-slice shipment ownership:

- one active shipment per order
- replacement shipment creation is allowed only when the last shipment is `CANCELLED` or `RETURNED`
- split shipments are intentionally deferred

## Scenario metadata

The built-in carrier reads optional metadata for deterministic local integration:

- `shippingScenario=create_exception`
- `shippingScenario=ready`
- `shippingScenario=in_transit`
- `shippingScenario=out_for_delivery`
- `shippingScenario=delivered`
- `shippingScenario=returned`
- `shippingScenario=exception`

These scenarios are primarily used for local integration, demos, and deterministic tests until real carrier callbacks exist.
