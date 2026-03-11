package com.cloudmedia.identity.events;

public interface IdentityEventPublisher {
	void publish(IdentityEventEnvelope event);
}
