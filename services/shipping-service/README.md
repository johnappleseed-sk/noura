# shipping-service

Production shipping abstraction service for shipping method discovery, rule-based quoting, shipment records, and fulfillment-ready shipment status hooks.

## Exposed endpoints

- `GET /api/v1/shipping/methods` (legacy alias: `/api/shipping/methods`)
- `POST /api/v1/shipping/quotes` (legacy alias: `/api/shipping/quotes`)
- `POST /api/v1/shipping/shipments` (legacy alias: `/api/shipping/shipments`)
- `GET /api/v1/shipping/shipments/{shipmentId}` (legacy alias: `/api/shipping/shipments/{shipmentId}`)
- `GET /api/v1/shipping/shipments/order/{orderId}` (legacy alias: `/api/shipping/shipments/order/{orderId}`)
- `POST /internal/shipping/shipments/status-update`
- `GET /api/v1/admin/merchants`
- `GET /api/v1/admin/merchants/{merchantId}`
- `POST /api/v1/admin/merchants`
- `PATCH /api/v1/admin/merchants/{merchantId}/status`
- `GET /api/v1/admin/stores`
- `GET /api/v1/admin/stores/{storeId}`
- `POST /api/v1/admin/stores`
- `PATCH /api/v1/admin/stores/{storeId}/status`
- `GET /api/v1/admin/stores/{storeId}/location`
- `PUT /api/v1/admin/stores/{storeId}/location`
- `GET /api/v1/stores`
- `GET /api/v1/stores/nearest`
- `PUT /api/v1/stores/preferred/{storeId}`
- `POST /api/v1/stores`
- `PUT /api/v1/stores/{storeId}`
- `DELETE /api/v1/stores/{storeId}`
- `GET /api/v1/admin/service-areas`
- `GET /api/v1/admin/service-areas/{serviceAreaId}`
- `POST /api/v1/admin/service-areas`
- `PUT /api/v1/admin/service-areas/{serviceAreaId}`
- `DELETE /api/v1/admin/service-areas/{serviceAreaId}`
- `POST /api/v1/admin/service-areas/{serviceAreaId}/activate`
- `POST /api/v1/admin/service-areas/{serviceAreaId}/deactivate`
- `POST /api/v1/admin/service-areas/validate`

## Scope (v1)

- Resolves shipping methods for address/cart snapshots through a carrier abstraction.
- Calculates deterministic internal quotes through the built-in rule-based carrier.
- Creates and persists shipment records backed by immutable order/customer/address snapshots from `order-service`.
- Supports shipment status lookup, optional carrier refresh, and internal fulfillment-status updates.
- Owns the extracted merchant/store/service-area compatibility slice used by current admin-web store-ops pages.
- Keeps the carrier boundary ready for later FedEx/UPS/DHL/local courier adapters.

## Integration behavior

- Validates order existence and ownership by querying `order-service`.
- Uses the order shipping address as the shipment recipient snapshot in this first slice.
- Does not mutate order lifecycle state in this slice; it exposes derived fulfillment hooks instead.
- Uses `order-service` internal API access when `ORDER_SERVICE_INTERNAL_API_KEY` is configured while still supporting owner-based reads through forwarded customer identity.
- Shipping now owns the temporary fulfillment-network compatibility model for merchant/store coverage instead of routing those pages through the retired legacy `app-service`.

## Quote rules

Built-in carrier code: `rule-based`

Methods:

- `standard`
  - available up to `30kg`
  - free when cart subtotal reaches `SHIPPING_RULE_BASED_FREE_STANDARD_THRESHOLD`
  - otherwise base `4.99` with surcharge after the first `1kg`
- `express`
  - available up to `20kg`
  - base `9.99` with surcharge after the first `0.5kg`
- `same_day`
  - available only in `SHIPPING_RULE_BASED_SAME_DAY_CITIES`
  - country must match `SHIPPING_RULE_BASED_SAME_DAY_COUNTRY_CODE`
  - available up to `10kg`

## Shipment lifecycle

- `CREATED`: internal record initialized before carrier response normalization.
- `LABEL_CREATED`: carrier label/tracking identifier exists.
- `READY_FOR_FULFILLMENT`: shipment is ready for warehouse or courier handoff.
- `IN_TRANSIT`: shipment is moving through the carrier network.
- `OUT_FOR_DELIVERY`: final-mile delivery attempt is underway.
- `DELIVERED`: shipment arrived successfully.
- `EXCEPTION`: carrier or fulfillment issue requires attention.
- `RETURNED`: shipment entered a return path.
- `CANCELLED`: shipment was canceled before completion.

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

## Scenario metadata

The rule-based carrier reads optional shipment metadata for deterministic local integration:

- `shippingScenario=create_exception` to force shipment creation into `EXCEPTION`
- `shippingScenario=ready`
- `shippingScenario=in_transit`
- `shippingScenario=out_for_delivery`
- `shippingScenario=delivered`
- `shippingScenario=returned`
- `shippingScenario=exception`

These scenarios are applied when the service refreshes carrier state or when the internal adapter creates a shipment.

## Local run

```bash
cd services/shipping-service
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
- `SHIPPING_RULE_BASED_CARRIER_CODE`
- `SHIPPING_RULE_BASED_DISPLAY_NAME`
- `SHIPPING_RULE_BASED_FREE_STANDARD_THRESHOLD`
- `SHIPPING_RULE_BASED_SAME_DAY_COUNTRY_CODE`
- `SHIPPING_RULE_BASED_SAME_DAY_CITIES`
