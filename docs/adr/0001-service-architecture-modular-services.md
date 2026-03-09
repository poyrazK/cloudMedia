# ADR-0001: Service architecture uses modular services

- Status: Accepted
- Date: 2026-03-09

## Context
CloudMedia MVP needs to move fast while supporting media processing, social interactions, livestreaming, and policy enforcement.

## Decision
Use a modular service architecture with clear domain ownership and service boundaries.

## Consequences
- Pros: parallel team execution, clear ownership, easier future scaling
- Pros: high-throughput and domain-heavy paths can evolve independently
- Cons: more operational complexity than a pure monolith
- Mitigation: strict API/event contracts and no cross-service database access
