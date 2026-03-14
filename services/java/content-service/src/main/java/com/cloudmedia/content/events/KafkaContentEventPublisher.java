package com.cloudmedia.content.events;

import java.util.concurrent.CompletionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

public class KafkaContentEventPublisher implements ContentEventPublisher {

	private static final Logger LOGGER = LoggerFactory.getLogger(KafkaContentEventPublisher.class);

	private final KafkaTemplate<String, Object> kafkaTemplate;

	private final ContentEventsKafkaProperties properties;

	public KafkaContentEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
			ContentEventsKafkaProperties properties) {
		this.kafkaTemplate = kafkaTemplate;
		this.properties = properties;
	}

	@Override
	public void publish(ContentEventEnvelope event) {
		String topic = topicFor(event.eventType());
		try {
			kafkaTemplate.send(topic, event.entityId(), event).join();
			LOGGER.debug("Published content event type={} id={} topic={}", event.eventType(), event.eventId(), topic);
		} catch (CompletionException exception) {
			Throwable cause = exception.getCause() != null ? exception.getCause() : exception;
			throw new IllegalStateException("Failed to publish content event " + event.eventType(), cause);
		}
	}

	String topicFor(String eventType) {
		if (!eventType.startsWith("content.")) {
			throw new IllegalArgumentException("Unsupported content event type " + eventType);
		}
		return properties.getTopicPrefix() + "." + eventType.substring("content.".length());
	}
}
