package com.cloudmedia.policy.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.cloudmedia.policy.api.policy.dto.UpdateContentPolicyRequest;
import com.cloudmedia.policy.api.policy.dto.UpdateModerationStateRequest;
import com.cloudmedia.policy.events.PolicyChangedPayload;
import com.cloudmedia.policy.events.PolicyEventPublisher;
import com.cloudmedia.policy.persistence.entity.ModerationState;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PolicyEventPublisherTest {

	@Autowired
	private ContentPolicyService contentPolicyService;

	@MockBean
	private PolicyEventPublisher policyEventPublisher;

	@Test
	void updateContentPolicyPublishesPolicyChangedEvent() {
		contentPolicyService.updateContentPolicy("content-event-1",
				new UpdateContentPolicyRequest(true, List.of("US", "TR"), List.of("RU")));

		verify(policyEventPublisher).publishPolicyChanged(any(PolicyChangedPayload.class), any());
	}

	@Test
	void updateModerationStatePublishesPolicyChangedEvent() {
		contentPolicyService.updateModerationState("content-event-2",
				new UpdateModerationStateRequest(ModerationState.HIDDEN));

		verify(policyEventPublisher).publishPolicyChanged(any(PolicyChangedPayload.class), any());
	}
}
