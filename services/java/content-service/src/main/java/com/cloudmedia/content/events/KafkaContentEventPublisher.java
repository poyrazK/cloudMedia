package com.cloudmedia.content.events;

import java.time.Instant;
import java.util.UUID;
import org.springframework.kafka.core.KafkaTemplate;

public class KafkaContentEventPublisher implements ContentEventPublisher {

	private static final int EVENT_VERSION = 1;

	private final KafkaTemplate<String, Object> kafkaTemplate;

	private final ContentEventsProperties properties;

	public KafkaContentEventPublisher(KafkaTemplate<String, Object> kafkaTemplate, ContentEventsProperties properties) {
		this.kafkaTemplate = kafkaTemplate;
		this.properties = properties;
	}

	@Override
	public void publishContentPublished(ContentPublishedPayload payload, String traceId) {
		ContentEventEnvelope envelope = new ContentEventEnvelope(UUID.randomUUID().toString(), "content.published",
				EVENT_VERSION, Instant.now(), "content-service", "content", payload.contentId(),
				resolveTraceId(traceId), payload);
		kafkaTemplate.send(properties.getTopics().getContentPublished(), payload.contentId(), envelope);
	}

	private String resolveTraceId(String traceId) {
		if (traceId != null && !traceId.isBlank()) {
			return traceId;
		}
		return "req_" + UUID.randomUUID();
	}
}
