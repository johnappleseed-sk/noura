# Domain Map

## Core Bounded Contexts

### Identity and Access
Owns:
- authentication
- token issuance
- federation
- service identity

### Admin Governance
Owns:
- RBAC
- approvals
- admin capability model
- immutable governance audit

### Merchant Network
Owns:
- merchants
- organizations
- contracts
- stores
- store membership

### Catalog Governance
Owns:
- master products
- master SKUs
- categories
- attribute definitions
- product submissions
- review decisions
- deduplication

### Catalog Reference / Assortment
Owns:
- merchant/store references to approved catalog items
- local visibility or publish state
- allowed SKU subset

Near term:
- may remain inside catalog or merchant boundary

### Inventory Availability
Owns:
- inventory nodes
- stock positions
- ledger
- reservations
- availability state

### Pricing and Promotion
Owns:
- price books
- contract pricing
- promotions
- coupons

### Customer Profile
Owns:
- customer business identity
- addresses
- preferences
- consents

### Cart / Checkout / Order / Payment / Fulfillment
Own:
- transaction state
- orchestration
- order system of record
- money movement
- shipment and return state

Near term:
- remain inside `commerce-core`

### Search Discovery
Owns:
- denormalized search documents
- facets
- autocomplete

Does not own:
- catalog truth
- inventory truth
- pricing truth

### Notification
Owns:
- templates
- delivery jobs
- preferences
- delivery logs

### Analytics and Reporting
Owns:
- marts
- aggregates
- reports

## Global Ownership Rules
- Catalog is the only creator of global product identity.
- Merchant/store domains reference approved catalog records.
- Inventory is keyed to canonical SKU identity.
- Orders snapshot the business data they need.
- Search and analytics are projections only.
