package com.cloudmedia.policy.application;

import com.cloudmedia.policy.api.policy.dto.UpdateContentPolicyRequest;
import com.cloudmedia.policy.error.ApiException;
import com.cloudmedia.policy.persistence.entity.ContentPolicyEntity;
import com.cloudmedia.policy.persistence.entity.ModerationState;
import com.cloudmedia.policy.persistence.repository.ContentPolicyRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class ContentPolicyServiceIntegrationTest {

	@Autowired
	private ContentPolicyService contentPolicyService;

	@Autowired
	private ContentPolicyRepository contentPolicyRepository;

	@Test
	void updateCreatesPolicyWhenMissing() {
		var response = contentPolicyService.updateContentPolicy("cnt_1",
				new UpdateContentPolicyRequest(true, List.of("tr", "de"), List.of("us")));

		assertEquals("cnt_1", response.contentId());
		assertTrue(response.ageRestricted());
		assertEquals(List.of("TR", "DE"), response.geoAllowList());
		assertEquals(List.of("US"), response.geoBlockList());
		assertEquals(ModerationState.VISIBLE, response.moderationState());
		assertNotNull(response.createdAt());
		assertNotNull(response.updatedAt());
	}

	@Test
	void updatePreservesOmittedFieldsAndClearsEmptyLists() {
		savePolicy("cnt_2", false, "TR,DE", "US");

		var response = contentPolicyService.updateContentPolicy("cnt_2",
				new UpdateContentPolicyRequest(true, List.of(), null));

		assertTrue(response.ageRestricted());
		assertEquals(List.of(), response.geoAllowList());
		assertEquals(List.of("US"), response.geoBlockList());
	}

	@Test
	void updateRejectsGeoOverlap() {
		ApiException exception = assertThrows(ApiException.class, () -> contentPolicyService
				.updateContentPolicy("cnt_3", new UpdateContentPolicyRequest(false, List.of("TR"), List.of("TR"))));

		assertEquals("POLICY_GEO_CONFLICT", exception.getCode());
	}

	private void savePolicy(String contentId, boolean ageRestricted, String allowList, String blockList) {
		ContentPolicyEntity entity = new ContentPolicyEntity();
		entity.setContentId(contentId);
		entity.setAgeRestricted(ageRestricted);
		entity.setGeoAllowList(allowList);
		entity.setGeoBlockList(blockList);
		entity.setModerationState(ModerationState.VISIBLE);
		contentPolicyRepository.saveAndFlush(entity);
	}
}
