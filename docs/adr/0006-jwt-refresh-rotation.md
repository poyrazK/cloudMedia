# ADR-0006: Auth tokens use JWT + refresh rotation

- Status: Accepted
- Date: 2026-03-09

## Context
The platform needs secure stateless access tokens and session control across devices.

## Decision
Use short-lived JWT access tokens (15 minutes) and rotating refresh tokens (30 days) with reuse detection and token-family revocation.

## Consequences
- Pros: strong security posture for session hijack scenarios
- Pros: scalable API auth verification
- Cons: additional token/session lifecycle complexity
- Mitigation: centralized session management and clear revocation APIs
