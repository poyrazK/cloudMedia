package com.cloudmedia.policy.application;

import com.cloudmedia.policy.api.policy.dto.EvaluateContentPolicyRequest;
import com.cloudmedia.policy.persistence.entity.ContentPolicyEntity;
import com.cloudmedia.policy.persistence.entity.ModerationState;
import com.cloudmedia.policy.persistence.repository.ContentPolicyRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class ContentPolicyEvaluationServiceIntegrationTest {

	@Autowired
	private ContentPolicyService contentPolicyService;

	@Autowired
	private ContentPolicyRepository contentPolicyRepository;

	@Test
	void evaluateAllowsContentWhenNoPolicyExists() {
		var response = contentPolicyService.evaluateContentPolicy("cnt_1",
				new EvaluateContentPolicyRequest(null, null));

		assertTrue(response.allowed());
		assertEquals(List.of(), response.reasonCodes());
		assertEquals(ModerationState.VISIBLE, response.moderationState());
	}

	@Test
	void evaluateDeniesRemovedContentFirst() {
		savePolicy("cnt_2", true, "TR", "US", ModerationState.REMOVED);

		var response = contentPolicyService.evaluateContentPolicy("cnt_2",
				new EvaluateContentPolicyRequest("TR", true));

		assertFalse(response.allowed());
		assertEquals(List.of("MODERATION_REMOVED"), response.reasonCodes());
	}

	@Test
	void evaluateDeniesAgeRestrictedContentWithoutVerification() {
		savePolicy("cnt_3", true, "", "", ModerationState.VISIBLE);

		var response = contentPolicyService.evaluateContentPolicy("cnt_3",
				new EvaluateContentPolicyRequest(null, false));

		assertFalse(response.allowed());
		assertEquals(List.of("AGE_RESTRICTED"), response.reasonCodes());
	}

	@Test
	void evaluateDeniesBlockedCountry() {
		savePolicy("cnt_4", false, "", "TR", ModerationState.VISIBLE);

		var response = contentPolicyService.evaluateContentPolicy("cnt_4",
				new EvaluateContentPolicyRequest("tr", true));

		assertFalse(response.allowed());
		assertEquals(List.of("GEO_BLOCKED"), response.reasonCodes());
	}

	@Test
	void evaluateDeniesWhenAllowListExistsAndCountryMissing() {
		savePolicy("cnt_5", false, "TR,DE", "", ModerationState.VISIBLE);

		var response = contentPolicyService.evaluateContentPolicy("cnt_5",
				new EvaluateContentPolicyRequest(null, true));

		assertFalse(response.allowed());
		assertEquals(List.of("GEO_NOT_ALLOWED"), response.reasonCodes());
	}

	@Test
	void evaluateAllowsCountryInAllowList() {
		savePolicy("cnt_6", false, "TR,DE", "", ModerationState.VISIBLE);

		var response = contentPolicyService.evaluateContentPolicy("cnt_6",
				new EvaluateContentPolicyRequest("de", true));

		assertTrue(response.allowed());
		assertEquals(List.of(), response.reasonCodes());
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
