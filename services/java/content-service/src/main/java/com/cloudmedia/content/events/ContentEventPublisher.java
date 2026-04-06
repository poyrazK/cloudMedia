package com.cloudmedia.content.events;

public interface ContentEventPublisher {

	void publishContentPublished(ContentPublishedPayload payload, String traceId);
}
