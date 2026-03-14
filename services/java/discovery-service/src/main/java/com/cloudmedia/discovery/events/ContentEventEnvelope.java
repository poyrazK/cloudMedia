package com.cloudmedia.discovery.events;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

public record ContentEventEnvelope(String eventId, String eventType, int eventVersion, Instant occurredAt,
		String producer, String entityType, String entityId, String traceId, JsonNode payload) {
}
