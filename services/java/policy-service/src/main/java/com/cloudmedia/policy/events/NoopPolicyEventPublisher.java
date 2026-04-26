package com.cloudmedia.policy.events;

public class NoopPolicyEventPublisher implements PolicyEventPublisher {

	@Override
	public void publishPolicyChanged(PolicyChangedPayload payload, String traceId) {
		// No-op implementation for when Kafka is disabled
	}
}
