package com.cloudmedia.policy.api.policy;

import com.cloudmedia.policy.persistence.entity.ContentPolicyEntity;
import com.cloudmedia.policy.persistence.entity.ModerationState;
import com.cloudmedia.policy.persistence.repository.ContentPolicyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PolicyEvaluationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ContentPolicyRepository contentPolicyRepository;

	@Test
	void evaluateAllowsContentWhenPolicyMissing() throws Exception {
		mockMvc.perform(post("/v1/policy/content/cnt_1/evaluate").contentType(MediaType.APPLICATION_JSON)
				.header("X-Request-Id", "req_eval_1").content("{}")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.allowed").value(true)).andExpect(jsonPath("$.data.reasonCodes").isEmpty())
				.andExpect(jsonPath("$.meta.requestId").value("req_eval_1"));
	}

	@Test
	void evaluateDeniesRemovedContent() throws Exception {
		savePolicy("cnt_2", true, "TR", "US", ModerationState.REMOVED);

		mockMvc.perform(post("/v1/policy/content/cnt_2/evaluate").contentType(MediaType.APPLICATION_JSON).content("""
				{
				  "countryCode": "TR",
				  "ageVerified": true
				}
				""")).andExpect(status().isOk()).andExpect(jsonPath("$.data.allowed").value(false))
				.andExpect(jsonPath("$.data.reasonCodes[0]").value("MODERATION_REMOVED"));
	}

	@Test
	void evaluateRejectsInvalidCountryCode() throws Exception {
		mockMvc.perform(post("/v1/policy/content/cnt_3/evaluate").contentType(MediaType.APPLICATION_JSON).content("""
				{
				  "countryCode": "TUR"
				}
				""")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
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
