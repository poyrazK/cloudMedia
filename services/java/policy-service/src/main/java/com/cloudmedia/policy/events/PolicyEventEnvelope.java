package com.cloudmedia.policy.events;

import java.time.Instant;

public record PolicyEventEnvelope(String eventId, String eventType, int eventVersion, Instant occurredAt,
		String producer, String entityType, String entityId, String traceId, Object payload) {
}
