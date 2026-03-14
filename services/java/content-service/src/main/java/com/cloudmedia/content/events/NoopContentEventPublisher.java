package com.cloudmedia.content.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoopContentEventPublisher implements ContentEventPublisher {

	private static final Logger LOGGER = LoggerFactory.getLogger(NoopContentEventPublisher.class);

	@Override
	public void publish(ContentEventEnvelope event) {
		LOGGER.debug("Noop content event publisher dropped event type={} id={}", event.eventType(), event.eventId());
	}
}
