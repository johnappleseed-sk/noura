# Platform Scripts

Local platform bootstrap scripts for the extracted NOURA stack.

## Files

- `docker-compose.local.yml`
  - shared infrastructure only: PostgreSQL, Redis, Kafka, Keycloak, plus the legacy compatibility service definitions retained for reference
- `.env.example`
  - baseline local ports and credentials
- `bootstrap-local-db.sh`
  - prepares the shared local PostgreSQL schema, applies missing service foundation tables when Flyway baselining would otherwise skip them, patches inventory audit drift, and seeds one demo product
- `run-local.sh`
  - starts infrastructure, bootstraps the database, launches extracted services in dependency order, rebuilds the search projection, and starts `apps/storefront-web` plus `apps/admin-web`
- `stop-local.sh`
  - stops the known NOURA local listeners on the standard service ports and optionally tears down Docker infrastructure with `--down-infra`

## Recommended Usage

From the repository root:

```bash
./platform/scripts/run-local.sh
```

To stop local processes:

```bash
./platform/scripts/stop-local.sh
```

To stop processes and shared infrastructure:

```bash
./platform/scripts/stop-local.sh --down-infra
```

## Local Runtime Notes

- The startup flow assumes `JDK 25`, Docker, Maven, npm, `curl`, and `screen` are available on the host.
- If `screen` is present, the launcher uses detached `screen` sessions so the services survive after the startup shell exits.
- The database bootstrap seeds one demo catalog/search/pricing/inventory record:
  - product: `Hydrating Glow Serum`
  - search query: `glow`
- This bootstrap is intentionally local-only. It compensates for the current shared-schema/Flyway baseline behavior and should not be treated as the production migration model.
