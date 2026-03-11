package com.cloudmedia.identity.events;

import java.time.Instant;

public record IdentityEventEnvelope(String eventId, String eventType, int eventVersion, Instant occurredAt,
		String producer, String entityType, String entityId, String traceId, Object payload) {
}
