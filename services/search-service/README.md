# search-service

Search-discovery extraction slice for NOURA.

This service currently owns:
- predictive search suggestions
- trend tags for discovery pages

It is intentionally read-only and reuses canonical catalog tables (`products`, `brands`, `stores`, `categories`) as a projection source.

## Exposed endpoints

- `GET /api/v1/search/predictive?q=...&scope=all|products|stores|brands`
- `GET /api/v1/search/trend-tags`

Legacy compatibility is handled in gateway:
- `/api/search/**` -> rewritten to `/api/v1/search/**`

## Local run

```bash
cd services/search-service
mvn spring-boot:run
```

Required environment variables:
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `API_VERSION_PREFIX` (optional, defaults to `/api/v1`)

## Notes

- Search remains a projection service, not system of record.
- Catalog owns product truth; search reads only.
