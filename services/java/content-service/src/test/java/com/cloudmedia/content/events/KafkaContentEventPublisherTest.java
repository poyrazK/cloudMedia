package com.cloudmedia.content.events;

import java.time.LocalDateTime;
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

class KafkaContentEventPublisherTest {

	@Test
	void publishContentPublishedSendsEnvelopeToConfiguredTopic() {
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
		ContentEventsProperties properties = new ContentEventsProperties();
		properties.getTopics().setContentPublished("cloudmedia.content.published");
		KafkaContentEventPublisher publisher = new KafkaContentEventPublisher(kafkaTemplate, properties);

		publisher.publishContentPublished(new ContentPublishedPayload("cnt_1", "chn_1", "Title", "Description", "VIDEO",
				"PUBLIC", LocalDateTime.parse("2026-03-14T12:00:00")), "req_123");

		ProducerRecord<String, Object> record = mockProducer.history().getFirst();
		assertEquals("cloudmedia.content.published", record.topic());
		assertEquals("cnt_1", record.key());
		ContentEventEnvelope envelope = (ContentEventEnvelope) record.value();
		assertEquals("content.published", envelope.eventType());
		assertEquals(1, envelope.eventVersion());
		assertEquals("content-service", envelope.producer());
		assertEquals("content", envelope.entityType());
		assertEquals("cnt_1", envelope.entityId());
		assertEquals("req_123", envelope.traceId());
		assertNotNull(envelope.eventId());
		assertNotNull(envelope.occurredAt());
	}

	@Test
	void publishContentUpdatedSendsEnvelopeToConfiguredTopic() {
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
		ContentEventsProperties properties = new ContentEventsProperties();
		properties.getTopics().setContentUpdated("cloudmedia.content.updated");
		KafkaContentEventPublisher publisher = new KafkaContentEventPublisher(kafkaTemplate, properties);

		publisher.publishContentUpdated(
				new ContentUpdatedPayload("cnt_2", "chn_2", "Updated Title", "VIDEO", "UNLISTED"), "req_456");

		assertEquals(1, mockProducer.history().size());
		ProducerRecord<String, Object> record = mockProducer.history().getFirst();
		assertEquals("cloudmedia.content.updated", record.topic());
		assertEquals("cnt_2", record.key());
		ContentEventEnvelope envelope = (ContentEventEnvelope) record.value();
		assertEquals("content.updated", envelope.eventType());
		assertEquals(1, envelope.eventVersion());
		assertEquals("content-service", envelope.producer());
		assertEquals("content", envelope.entityType());
		assertEquals("cnt_2", envelope.entityId());
		assertEquals("req_456", envelope.traceId());
		assertNotNull(envelope.eventId());
		assertNotNull(envelope.occurredAt());
		ContentUpdatedPayload payload = (ContentUpdatedPayload) envelope.payload();
		assertEquals("cnt_2", payload.contentId());
		assertEquals("chn_2", payload.channelId());
		assertEquals("Updated Title", payload.title());
		assertEquals("VIDEO", payload.contentType());
		assertEquals("UNLISTED", payload.visibility());
	}
}
