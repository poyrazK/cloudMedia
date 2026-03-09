# ADR-0002: API style is REST-first

- Status: Accepted
- Date: 2026-03-09

## Context
MVP requires rapid delivery, stable contracts, and straightforward observability.

## Decision
Use REST-first APIs for public and internal service endpoints. Use versioned routes (`/v1`) and a unified error envelope.

## Consequences
- Pros: faster implementation and easier debugging
- Pros: simpler caching and auth patterns
- Cons: some composite screens may require additional endpoints
- Mitigation: add purpose-built feed/search endpoints where needed
