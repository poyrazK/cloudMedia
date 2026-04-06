package com.cloudmedia.content.events;

public class NoopContentEventPublisher implements ContentEventPublisher {

	@Override
	public void publishContentPublished(ContentPublishedPayload payload, String traceId) {
		// intentionally no-op when content event publishing is disabled
	}
}
