# customer-service

Production customer account service for storefront profile and address book flows.

## Exposed endpoints

- `GET /api/v1/account/profile` (legacy alias: `/api/account/profile`)
- `PUT /api/v1/account/profile` (legacy alias: `/api/account/profile`)
- `GET /api/v1/account/addresses` (legacy alias: `/api/account/addresses`)
- `GET /api/v1/account/addresses/{addressId}` (legacy alias: `/api/account/addresses/{addressId}`)
- `POST /api/v1/account/addresses` (legacy alias: `/api/account/addresses`)
- `PUT /api/v1/account/addresses/{addressId}` (legacy alias: `/api/account/addresses/{addressId}`)
- `DELETE /api/v1/account/addresses/{addressId}` (legacy alias: `/api/account/addresses/{addressId}`)
- `POST /api/v1/account/addresses/{addressId}/set-default?type=SHIPPING|BILLING|BOTH` (type optional, defaults to `BOTH`)
- `GET /internal/customers/{customerId}`
- `GET /internal/customers/by-subject/{externalSubject}`

## Identity handling

- Preferred identity source: `X-Auth-Subject` forwarded by gateway auth.
- Fallback for local/dev when auth forwarding is disabled: bearer token fingerprint from `Authorization`.
- If no identity is resolvable, account endpoints return `401`.

## Local run

```bash
cd services/customer-service
mvn spring-boot:run
```

Required environment variables:
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

Optional:
- `SERVER_PORT` (default `8080`)
- `APP_INTERNAL_API_KEY` for internal lookup endpoint protection

## Notes

- Profile row is created lazily on first account API call.
- Address defaults support separate shipping and billing defaults.
- Response payload keeps `defaultAddress` for storefront compatibility.
- Internal lookup endpoints require `X-Internal-Api-Key` only when `APP_INTERNAL_API_KEY` is configured.
