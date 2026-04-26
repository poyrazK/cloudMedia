# ADR 014: Policy Service Kafka Event Publishing

## Status

Accepted

## Date

2026-04-26

## Context

The policy-service MVP was missing Kafka event publishing for policy changes. The implementation plan specified adding:

1. Kafka dependency and configuration
2. Event model classes (PolicyChangedPayload, PolicyEventEnvelope)
3. PolicyEventPublisher interface with Kafka and Noop implementations
4. Integration into ContentPolicyService

These events are needed for:

- Propagating policy changes to downstream consumers (content-service, discovery-service)
- Keeping derived systems (search index, recommendations) in sync with policy updates
- Audit trail and observability of policy changes

## Decision

### Kafka Dependency

Added `spring-kafka` dependency to `policy-service/pom.xml`.

### Event Model

Created `PolicyChangedPayload` record with fields:

- `contentId` - the content this policy applies to
- `ageRestricted` - age restriction flag
- `geoAllowList` / `geoBlockList` - geographic policy lists
- `moderationState` - current moderation state
- `occurredAt` - timestamp of the change

Created `PolicyEventEnvelope` matching the standard envelope contract with:

- `eventId`, `eventType`, `eventVersion`, `occurredAt`
- `producer` ("policy-service"), `entityType` ("content"), `entityId`
- `traceId` - request trace or auto-generated fallback

### Publisher Interface

Created `PolicyEventPublisher` interface with `publishPolicyChanged(payload, traceId)` method.

Two implementations:

- `KafkaPolicyEventPublisher` - sends to `cloudmedia.policy.changed` topic using `KafkaTemplate`
- `NoopPolicyEventPublisher` - no-op fallback when Kafka is disabled

### Configuration

Created `PolicyEventsProperties` with configurable topic (default: `cloudmedia.policy.changed`) and `PolicyEventsConfiguration` with conditional bean wiring:

- `KafkaPolicyEventPublisher` when `cloudmedia.policy.events.enabled=true`
- `NoopPolicyEventPublisher` as fallback

### Service Integration

Updated `ContentPolicyService` to inject `PolicyEventPublisher` and call `publishPolicyChanged` after both `updateContentPolicy` and `updateModerationState`.

## Consequences

### Positive

- Policy changes are now broadcast to downstream services via Kafka
- Standard envelope format maintains consistency across all services
- No-op fallback allows policy-service to run without Kafka during development
- Configuration-based enablement allows per-environment control

### Negative

- Adds Kafka as a runtime dependency for full functionality
- Event publishing is fire-and-forget (no retries in MVP)

### Neutral

- Event payload aligned with existing `policy.changed` schema in kafka-event-catalog.md
- Backward compatible additions to existing policy-service endpoints

## Alternatives Considered

### Alternative 1: Polling Instead of Events

Have consumers poll policy-service for changes.

**Why rejected:** Polling is inefficient and adds latency. Event-driven architecture per ADR-004 is the established pattern.

### Alternative 2: Synchronous HTTP Callbacks

Call downstream services directly via HTTP when policies change.

**Why rejected:** Tight coupling, no durability if downstream is down, no fan-out capability. Kafka provides decoupled, durable, fan-out delivery.

## Implementation Notes

- New files: 7 (4 implementation + 3 test)
- New tests: `PolicyEventPublisherTest` (integration), `KafkaPolicyEventPublisherTest` (unit), `NoopPolicyEventPublisherTest` (unit)
- All tests passing
