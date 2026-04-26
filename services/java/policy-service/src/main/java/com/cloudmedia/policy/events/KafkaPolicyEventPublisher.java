package com.cloudmedia.policy.events;

import java.time.Instant;
import java.util.UUID;
import org.springframework.kafka.core.KafkaTemplate;

public class KafkaPolicyEventPublisher implements PolicyEventPublisher {

	private static final int EVENT_VERSION = 1;

	private final KafkaTemplate<String, Object> kafkaTemplate;

	private final PolicyEventsProperties properties;

	public KafkaPolicyEventPublisher(KafkaTemplate<String, Object> kafkaTemplate, PolicyEventsProperties properties) {
		this.kafkaTemplate = kafkaTemplate;
		this.properties = properties;
	}

	@Override
	public void publishPolicyChanged(PolicyChangedPayload payload, String traceId) {
		PolicyEventEnvelope envelope = new PolicyEventEnvelope(UUID.randomUUID().toString(), "policy.changed",
				EVENT_VERSION, Instant.now(), "policy-service", "content", payload.contentId(), resolveTraceId(traceId),
				payload);
		kafkaTemplate.send(properties.getTopics().getPolicyChanged(), payload.contentId(), envelope);
	}

	private String resolveTraceId(String traceId) {
		if (traceId != null && !traceId.isBlank()) {
			return traceId;
		}
		return "req_" + UUID.randomUUID();
	}
}
