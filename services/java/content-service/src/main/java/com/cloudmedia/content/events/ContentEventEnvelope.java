package com.cloudmedia.content.events;

import java.time.Instant;

public record ContentEventEnvelope(String eventId, String eventType, int eventVersion, Instant occurredAt,
		String producer, String entityType, String entityId, String traceId, Object payload) {
}
