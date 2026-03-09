# ADR-0005: OpenSearch is derived search index

- Status: Accepted
- Date: 2026-03-09

## Context
Discovery and search quality require specialized retrieval capabilities. Primary transactional integrity must stay in relational storage.

## Decision
Use OpenSearch for search/discovery reads. Keep Postgres as source of truth and treat OpenSearch as a derived index updated by events.

## Consequences
- Pros: better relevance features and query capabilities
- Pros: independent optimization of search performance
- Cons: index drift risk
- Mitigation: idempotent upserts, alias-based reindexing, nightly reconciliation jobs
