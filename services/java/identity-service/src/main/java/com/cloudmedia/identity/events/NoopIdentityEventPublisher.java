package com.cloudmedia.identity.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NoopIdentityEventPublisher implements IdentityEventPublisher {

	private static final Logger LOGGER = LoggerFactory.getLogger(NoopIdentityEventPublisher.class);

	@Override
	public void publish(IdentityEventEnvelope event) {
		LOGGER.debug("Noop identity event publisher dropped event type={} id={}", event.eventType(), event.eventId());
	}
}
