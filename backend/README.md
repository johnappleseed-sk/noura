# NOURA Backend API

Spring Boot backend for NOURA commerce services.

## Security Setup

### JWT Secret (Required)

- `JWT_SECRET` must be provided through environment variables.
- In non-local profiles (`staging`, `prod`), startup fails if:
  - `JWT_SECRET` is missing
  - `JWT_SECRET` is shorter than 32 characters
  - `JWT_SECRET` looks like a default/placeholder value (for example contains `change-me` or `default`)
  - `JWT_SECRET` is a trivial repeated-character value

Generate a secure secret (example with Python):

```bash
python -c "import secrets; print(secrets.token_urlsafe(48))"
```

Set it before starting the app:

```bash
# Linux/macOS
export JWT_SECRET="<generated-secret>"

# Windows (cmd)
set JWT_SECRET=<generated-secret>
```

### Profiles

- `local` (default): `application-local.yml`
- `staging`: `application-staging.yml`
- `prod`: `application-prod.yml`

### CORS Allowlist

- `CORS_ALLOWED_ORIGINS` is environment-driven.
- In non-local profiles (`staging`, `prod`), startup fails if:
  - no origins are configured
  - any wildcard origin is used (for example `*`)

Example:

```bash
export CORS_ALLOWED_ORIGINS="https://admin.example.com,https://ops.example.com"
```

### Rate Limit Controls

- `RATE_LIMIT_CAPACITY`
- `RATE_LIMIT_REFILL_TOKENS`
- `RATE_LIMIT_REFILL_MINUTES`
- `RATE_LIMIT_KEY_TTL_MINUTES`
- `RATE_LIMIT_MAX_KEYS`
- `RATE_LIMIT_TRUST_FORWARDED_HEADERS`
- `RATE_LIMIT_FORWARDED_IP_HEADER` (default: `X-Forwarded-For`)
- `RATE_LIMIT_TRUSTED_PROXY_ADDRESSES` (default: `127.0.0.1,::1`)

Forwarded client IP headers are only used when:

- `RATE_LIMIT_TRUST_FORWARDED_HEADERS=true`
- request remote address is in `RATE_LIMIT_TRUSTED_PROXY_ADDRESSES`

Otherwise, rate limiting falls back to `remoteAddr`.

### Correlation IDs

- API accepts optional `X-Correlation-ID` request header.
- If missing or invalid, backend generates a UUID correlation id.
- Response always includes `X-Correlation-ID`.
- Correlation id is added to MDC as `correlationId` for log correlation.

### Structured Logging

- Backend console logs are emitted as single-line JSON from `logback-spring.xml`.
- Fields include: `timestamp`, `level`, `app`, `logger`, `thread`, `correlationId`, `message`, `exception`.
- `correlationId` aligns with the `X-Correlation-ID` response header for request tracing.

## Version

- Current API release: `1.1.0`
- Swagger UI: `/swagger-ui`

## New in 1.1.0

### Checkout

- `POST /api/v1/checkout`
- `POST /api/v1/checkout/steps/confirm`
  - Supports optional `idempotencyKey` in request body (max 128 chars).
  - If the same user retries with the same key, backend returns the original order.
  - Coupon validation is centralized in `PricingService` (used by both cart preview and checkout).
  - Only one coupon code is supported per request.
  - Coupons can be bounded by optional `valid_from` and `valid_until` windows.

Example:

```json
{
  "fulfillmentMethod": "DELIVERY",
  "shippingAddressSnapshot": "123 Main St",
  "paymentReference": "PAY-123",
  "couponCode": "WELCOME10",
  "b2bInvoice": false,
  "idempotencyKey": "checkout-2026-03-04-0001"
}
```

### Cart

- `DELETE /api/v1/cart/items`
  - Clears all cart items for the current user and resets cart store context.

Example:

```bash
curl -X DELETE "http://localhost:8080/api/v1/cart/items" \
  -H "Authorization: Bearer <JWT>"
```

### Notifications

- `GET /api/v1/notifications/me/unread-count`
- `PATCH /api/v1/notifications/{notificationId}/read`
- `PATCH /api/v1/notifications/me/read-all`

Examples:

```bash
curl "http://localhost:8080/api/v1/notifications/me/unread-count" \
  -H "Authorization: Bearer <JWT>"
```

```bash
curl -X PATCH "http://localhost:8080/api/v1/notifications/7d8f.../read" \
  -H "Authorization: Bearer <JWT>"
```

```bash
curl -X PATCH "http://localhost:8080/api/v1/notifications/me/read-all" \
  -H "Authorization: Bearer <JWT>"
```

### Orders

- `GET /api/v1/orders/{orderId}/timeline`
  - Returns chronological order status/refund progression for audit and support.

Example:

```bash
curl "http://localhost:8080/api/v1/orders/7d8f.../timeline" \
  -H "Authorization: Bearer <JWT>"
```
