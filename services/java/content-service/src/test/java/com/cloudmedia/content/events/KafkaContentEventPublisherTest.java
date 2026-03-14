package com.cloudmedia.content.events;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class KafkaContentEventPublisherTest {

	private final TestKafkaTemplate kafkaTemplate = new TestKafkaTemplate();

	private final ContentEventsKafkaProperties properties = new ContentEventsKafkaProperties();

	private KafkaContentEventPublisher publisher;

	@BeforeEach
	void setUp() {
		properties.setTopicPrefix("cloudmedia.content");
		publisher = new KafkaContentEventPublisher(kafkaTemplate, properties);
	}

	@Test
	void publishRoutesCreatedEventsToCreatedTopic() {
		ContentEventEnvelope event = event("content.created", "cnt_1");
		kafkaTemplate.result = CompletableFuture.completedFuture(null);

		publisher.publish(event);

		assertEquals("cloudmedia.content.created", kafkaTemplate.topic);
		assertEquals("cnt_1", kafkaTemplate.key);
		assertEquals(event, kafkaTemplate.value);
	}

	@Test
	void publishRoutesPublishedEventsToPublishedTopic() {
		ContentEventEnvelope event = event("content.published", "cnt_2");
		kafkaTemplate.result = CompletableFuture.completedFuture(null);

		publisher.publish(event);

		assertEquals("cloudmedia.content.published", kafkaTemplate.topic);
		assertEquals("cnt_2", kafkaTemplate.key);
		assertEquals(event, kafkaTemplate.value);
	}

	@Test
	void publishFailsRequestWhenKafkaSendFails() {
		ContentEventEnvelope event = event("content.updated", "cnt_3");
		kafkaTemplate.result = CompletableFuture.failedFuture(new RuntimeException("broker unavailable"));

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> publisher.publish(event));

		assertEquals("Failed to publish content event content.updated", exception.getMessage());
	}

	private ContentEventEnvelope event(String eventType, String entityId) {
		return new ContentEventEnvelope("evt_1", eventType, 1, Instant.parse("2026-03-14T12:00:00Z"), "content-service",
				"content", entityId, "req_1", new ContentUpdatedPayload(entityId, "chn_1", "Title", null, null));
	}

	static class TestKafkaTemplate extends KafkaTemplate<String, Object> {

		private String topic;

		private String key;

		private Object value;

		private CompletableFuture<?> result;

		TestKafkaTemplate() {
			super(mock(ProducerFactory.class));
		}

		@Override
		@SuppressWarnings("unchecked")
		public CompletableFuture<SendResult<String, Object>> send(String topic, String key, Object data) {
			this.topic = topic;
			this.key = key;
			this.value = data;
			return (CompletableFuture<SendResult<String, Object>>) result;
		}
	}
}
