# ADR-0004: Event backbone is Kafka

- Status: Accepted
- Date: 2026-03-09

## Context
Media processing, indexing, discovery updates, and lifecycle events require reliable asynchronous processing.

## Decision
Use Kafka as the event backbone. Require event versioning, idempotent consumers, and DLQ per consumer group.

## Consequences
- Pros: scalable async processing and replay support
- Pros: strong fit for event-driven indexing and media workflows
- Cons: consumer lag and topic governance overhead
- Mitigation: lag SLOs, replay runbooks, and strict partition key policy
