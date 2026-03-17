# AI Collaboration Rules

## Source of Truth
Treat the files in `docs/ai-memory/` as persistent project memory.

Do not contradict them unless:
- a real inconsistency is found
- a better design is required
- the change is explicitly explained

## Required Read Order
Read before architecture or implementation work:
1. `PROJECT_CONTEXT.md`
2. `RECENT_CHANGES.md`
3. `BUSINESS_MODEL.md`
4. `ARCHITECTURE.md`
5. `DOMAIN_MAP.md`
6. `SERVICE_CATALOG.md`
7. `MIGRATION_PLAN.md`
8. `ENGINEERING_PRINCIPLES.md`

## Default AI Behavior
- do not assume greenfield
- do not over-fragment services
- preserve the master catalog rule
- preserve the super inventory rule
- reuse existing useful code where practical
- prefer migration-safe recommendations

## Persistent Rules
- no duplicate global master products
- search is not a source of truth
- analytics is not a source of truth
- inventory is separate from catalog identity
- architecture must remain startup-realistic

## Implementation Guidance
- inspect the repo before proposing code changes
- align new work with Phase 1 service map
- prefer explicit tradeoffs over vague best practices
- update memory files when architecture changes materially

## Anti-Patterns
Do not:
- recommend full rewrites without migration steps
- invent services without clear ownership
- collapse catalog, inventory, pricing, and search into one truth model
- recommend trendy infrastructure without clear operational value
