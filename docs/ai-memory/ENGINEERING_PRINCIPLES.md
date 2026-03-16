# Engineering Principles

## Delivery Principles
- preserve working business value
- migrate in phases
- optimize for low-risk execution
- keep architecture ahead of entropy

## Code Structure
- package by domain
- keep domain logic independent from transport and persistence
- avoid shared business libraries
- no generic `common` dumping ground

## Service Boundaries
- one source of truth per aggregate
- no cross-service table writes
- no shared JPA entities across services
- contract-first for external and cross-service APIs

## Data Principles
- catalog is canonical for product identity
- inventory is canonical for stock
- pricing is canonical for commercial terms
- search and analytics are read models
- audit significant governance actions

## Operational Principles
- every service must emit metrics, traces, and structured logs
- correlation IDs are mandatory
- every critical release path needs rollback
- infra must be reproducible from code

## Security Principles
- OIDC/OAuth2 for identity
- RBAC for admin and operational access
- least privilege for services and databases
- externalized secrets only
- audit privileged mutations

## Testing Principles
- unit test domain logic
- integration test real infrastructure boundaries
- contract test APIs and events
- smoke test critical user journeys

## Documentation Principles
- keep memory files concise and current
- document decisions in ADRs
- do not let architecture live only in chat history

## Startup Bias
- choose boring, proven defaults
- add complexity only for a current or near-term problem
- prefer managed stateful infrastructure
