package com.cloudmedia.policy.application;

import com.cloudmedia.policy.api.policy.dto.UpdateModerationStateRequest;
import com.cloudmedia.policy.persistence.entity.ContentPolicyEntity;
import com.cloudmedia.policy.persistence.entity.ModerationState;
import com.cloudmedia.policy.persistence.repository.ContentPolicyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class ModerationPolicyServiceIntegrationTest {

	@Autowired
	private ContentPolicyService contentPolicyService;

	@Autowired
	private ContentPolicyRepository contentPolicyRepository;

	@Test
	void moderationUpdateCreatesPolicyWhenMissing() {
		var response = contentPolicyService.updateModerationState("cnt_1",
				new UpdateModerationStateRequest(ModerationState.HIDDEN));

		assertEquals("cnt_1", response.contentId());
		assertEquals(ModerationState.HIDDEN, response.moderationState());
		assertEquals(java.util.List.of(), response.geoAllowList());
		assertEquals(java.util.List.of(), response.geoBlockList());
	}

	@Test
	void moderationUpdatePreservesAgeAndGeoFields() {
		savePolicy("cnt_2", true, "TR,DE", "US", ModerationState.VISIBLE);

		var response = contentPolicyService.updateModerationState("cnt_2",
				new UpdateModerationStateRequest(ModerationState.REMOVED));

		assertTrue(response.ageRestricted());
		assertEquals(java.util.List.of("TR", "DE"), response.geoAllowList());
		assertEquals(java.util.List.of("US"), response.geoBlockList());
		assertEquals(ModerationState.REMOVED, response.moderationState());
	}

	private void savePolicy(String contentId, boolean ageRestricted, String allowList, String blockList,
			ModerationState moderationState) {
		ContentPolicyEntity entity = new ContentPolicyEntity();
		entity.setContentId(contentId);
		entity.setAgeRestricted(ageRestricted);
		entity.setGeoAllowList(allowList);
		entity.setGeoBlockList(blockList);
		entity.setModerationState(moderationState);
		contentPolicyRepository.saveAndFlush(entity);
	}
}
