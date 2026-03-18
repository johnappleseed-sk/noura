# API Error Response Standard

NOURA services use one common response envelope for both success and failure responses.

## Envelope

Successful responses include:

```json
{
  "success": true,
  "message": "Order placed",
  "data": {},
  "correlationId": "corr-123",
  "timestamp": "2026-03-18T15:00:00Z",
  "path": "/api/v1/checkout/place-order"
}
```

Failed responses include:

```json
{
  "success": false,
  "message": "Conflict",
  "error": {
    "code": "INSUFFICIENT_STOCK",
    "detail": "Available stock is below requested quantity",
    "validationErrors": null
  },
  "correlationId": "corr-123",
  "timestamp": "2026-03-18T15:00:00Z",
  "path": "/api/v1/checkout/validate"
}
```

## Standard Failure Categories

- Validation failures
  - HTTP `400`
  - message: `Validation failed`
  - code: `VALIDATION_ERROR` or `INVALID_REQUEST_BODY`
- Not found
  - HTTP `404`
  - message: `Resource not found`
  - code: service/domain specific not-found code
- Request rejected
  - HTTP `400`
  - message: `Request rejected`
  - code: service/domain specific business-rule code
- Unauthorized
  - HTTP `401`
  - message: `Unauthorized`
- Forbidden
  - HTTP `403`
  - message: `Forbidden`
- Conflict
  - HTTP `409`
  - message: `Conflict`
- Downstream or availability failures
  - HTTP `502`, `503`, and related statuses use the HTTP reason phrase as the top-level message
- Unexpected failures
  - HTTP `500`
  - message: `Internal server error`
  - code: `INTERNAL_SERVER_ERROR`
  - detail: `Unexpected error occurred`

## Validation Error Format

When field-level validation is available, the response includes `error.validationErrors`:

```json
{
  "success": false,
  "message": "Validation failed",
  "error": {
    "code": "VALIDATION_ERROR",
    "detail": "One or more request fields are invalid",
    "validationErrors": {
      "quantity": "must be greater than 0"
    }
  },
  "correlationId": "corr-123",
  "timestamp": "2026-03-18T15:00:00Z",
  "path": "/api/v1/cart/items"
}
```

## DTO Naming Rule

- Public API write models use `*Request`.
- Public API read models use `*Response`.
- Extra qualifiers such as `Internal`, `Legacy`, `BatchUpsert`, `Validation`, or `Summary` are allowed when they add scope clarity without removing the `Request` or `Response` suffix.
