package com.cloudmedia.policy.events;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KafkaPolicyEventPublisherTest {

	@Test
	void publishPolicyChangedSendsEnvelopeToConfiguredTopic() {
		MockProducer<String, Object> mockProducer = new MockProducer<>(true, new StringSerializer(),
				new JsonSerializer<>());
		ProducerFactory<String, Object> producerFactory = new ProducerFactory<>() {
			@Override
			public org.apache.kafka.clients.producer.Producer<String, Object> createProducer() {
				return mockProducer;
			}

			@Override
			public boolean transactionCapable() {
				return false;
			}

			@Override
			public Map<String, Object> getConfigurationProperties() {
				return new HashMap<>();
			}
		};
		KafkaTemplate<String, Object> kafkaTemplate = new KafkaTemplate<>(producerFactory);
		PolicyEventsProperties properties = new PolicyEventsProperties();
		properties.getTopics().setPolicyChanged("cloudmedia.policy.changed");
		KafkaPolicyEventPublisher publisher = new KafkaPolicyEventPublisher(kafkaTemplate, properties);

		publisher.publishPolicyChanged(new PolicyChangedPayload("cnt_1", true, java.util.List.of("TR", "DE"),
				java.util.List.of("RU"), "HIDDEN", Instant.parse("2026-03-14T12:00:00Z")), "req_789");

		ProducerRecord<String, Object> record = mockProducer.history().getFirst();
		assertEquals("cloudmedia.policy.changed", record.topic());
		assertEquals("cnt_1", record.key());
		PolicyEventEnvelope envelope = (PolicyEventEnvelope) record.value();
		assertEquals("policy.changed", envelope.eventType());
		assertEquals(1, envelope.eventVersion());
		assertEquals("policy-service", envelope.producer());
		assertEquals("content", envelope.entityType());
		assertEquals("cnt_1", envelope.entityId());
		assertEquals("req_789", envelope.traceId());
		assertNotNull(envelope.eventId());
		assertNotNull(envelope.occurredAt());
		PolicyChangedPayload payload = (PolicyChangedPayload) envelope.payload();
		assertEquals("cnt_1", payload.contentId());
		assertEquals(true, payload.ageRestricted());
		assertEquals(java.util.List.of("TR", "DE"), payload.geoAllowList());
		assertEquals(java.util.List.of("RU"), payload.geoBlockList());
		assertEquals("HIDDEN", payload.moderationState());
	}

	@Test
	void publishPolicyChangedGeneratesTraceIdWhenNull() {
		MockProducer<String, Object> mockProducer = new MockProducer<>(true, new StringSerializer(),
				new JsonSerializer<>());
		ProducerFactory<String, Object> producerFactory = new ProducerFactory<>() {
			@Override
			public org.apache.kafka.clients.producer.Producer<String, Object> createProducer() {
				return mockProducer;
			}

			@Override
			public boolean transactionCapable() {
				return false;
			}

			@Override
			public Map<String, Object> getConfigurationProperties() {
				return new HashMap<>();
			}
		};
		KafkaTemplate<String, Object> kafkaTemplate = new KafkaTemplate<>(producerFactory);
		PolicyEventsProperties properties = new PolicyEventsProperties();
		KafkaPolicyEventPublisher publisher = new KafkaPolicyEventPublisher(kafkaTemplate, properties);

		publisher.publishPolicyChanged(new PolicyChangedPayload("cnt_2", false, java.util.List.of(),
				java.util.List.of(), "VISIBLE", Instant.now()), null);

		ProducerRecord<String, Object> record = mockProducer.history().getFirst();
		PolicyEventEnvelope envelope = (PolicyEventEnvelope) record.value();
		assertNotNull(envelope.traceId());
		assertTrue(envelope.traceId().startsWith("req_"));
	}
}
