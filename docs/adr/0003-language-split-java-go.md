# ADR-0003: Language split is Java + Go

- Status: Accepted
- Date: 2026-03-09

## Context
Core business domains need strong ecosystem support and maintainability, while infra and realtime paths need high concurrency and low overhead.

## Decision
Use Java (Spring Boot) for core business/domain services and Go for infrastructure, control-plane, and high-throughput services.

## Consequences
- Pros: good fit for domain logic plus realtime/infra performance
- Pros: team specialization by service type
- Cons: dual-language operational complexity
- Mitigation: shared standards for APIs, events, telemetry, and CI quality gates
