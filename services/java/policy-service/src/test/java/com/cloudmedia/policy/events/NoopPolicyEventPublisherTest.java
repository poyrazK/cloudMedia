package com.cloudmedia.policy.events;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NoopPolicyEventPublisherTest {

	@Test
	void publishPolicyChangedDoesNothing() {
		NoopPolicyEventPublisher publisher = new NoopPolicyEventPublisher();
		PolicyChangedPayload payload = new PolicyChangedPayload("cnt_1", true, List.of("TR"), List.of("RU"), "HIDDEN",
				Instant.now());

		assertDoesNotThrow(() -> publisher.publishPolicyChanged(payload, "req_123"));
	}
}
