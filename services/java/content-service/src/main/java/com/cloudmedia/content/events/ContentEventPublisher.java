package com.cloudmedia.content.events;

public interface ContentEventPublisher {
	void publish(ContentEventEnvelope event);
}
