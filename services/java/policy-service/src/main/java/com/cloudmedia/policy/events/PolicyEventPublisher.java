package com.cloudmedia.policy.events;

public interface PolicyEventPublisher {

	void publishPolicyChanged(PolicyChangedPayload payload, String traceId);
}
