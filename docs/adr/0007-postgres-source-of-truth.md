# ADR-0007: Postgres is source of truth

- Status: Accepted
- Date: 2026-03-09

## Context
Core domains (identity, content metadata, social graph, policy) require transactional consistency.

## Decision
Use Postgres as the authoritative data store for transactional domains. Other stores (OpenSearch, Redis) are derived or ephemeral.

## Consequences
- Pros: strong consistency and mature relational tooling
- Pros: clear data ownership model
- Cons: schema evolution must be carefully managed
- Mitigation: migration discipline and domain schema boundaries
