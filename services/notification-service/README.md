# notification-service

Notification delivery service for storefront/account reads and trusted internal dispatch.

## Exposed endpoints

- `GET /api/v1/notifications/me`
- `GET /api/v1/notifications/me/unread-count`
- `PATCH /api/v1/notifications/me/read-all`
- `PATCH /api/v1/notifications/{notificationId}/read`
- `POST /api/v1/notifications/user/{userId}`
- `POST /api/v1/notifications/broadcast`
- `POST /internal/notifications`

## Local run

```bash
cd services/notification-service
mvn spring-boot:run
```

Required environment variables:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

Optional environment variables:

- `SERVER_PORT` (default `8080`)
- `APP_INTERNAL_API_KEY`

## Persistence notes

- Flyway owns `notification_messages` and runs automatically on startup.
- Notification enums are stored as strings, not ordinals.
- `V2__notification_audit_cleanup.sql` standardizes `updated_at` as a required database-managed timestamp and adds a composite recipient/status index for unread-count queries.
