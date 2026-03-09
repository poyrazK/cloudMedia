# ADR-0011: 4K access available to all users

- Status: Accepted
- Date: 2026-03-09

## Context
Product requirement is broad playback capability with no resolution restrictions by account type.

## Decision
Allow all users to request and play available renditions up to 4K when source and transcode outputs support it.

## Consequences
- Pros: consistent user experience and fewer entitlement edge cases
- Cons: increased transcode and delivery cost pressure
- Mitigation: queue prioritization and cost guardrails during high load
