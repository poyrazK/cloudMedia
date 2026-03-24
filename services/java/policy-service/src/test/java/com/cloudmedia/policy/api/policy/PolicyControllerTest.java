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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PolicyControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ContentPolicyRepository contentPolicyRepository;

	@Test
	void patchCreatesPolicy() throws Exception {
		mockMvc.perform(patch("/v1/policy/content/cnt_1").contentType(MediaType.APPLICATION_JSON)
				.header("X-Request-Id", "req_policy_1").content("""
						{
						  "ageRestricted": true,
						  "geoAllowList": ["tr", "de"],
						  "geoBlockList": ["us"]
						}
						""")).andExpect(status().isOk()).andExpect(jsonPath("$.data.contentId").value("cnt_1"))
				.andExpect(jsonPath("$.data.ageRestricted").value(true))
				.andExpect(jsonPath("$.data.geoAllowList[0]").value("TR"))
				.andExpect(jsonPath("$.data.geoBlockList[0]").value("US"))
				.andExpect(jsonPath("$.meta.requestId").value("req_policy_1"));
	}

	@Test
	void patchUpdatesExistingPolicyPartially() throws Exception {
		savePolicy("cnt_2", false, "TR,DE", "US");

		mockMvc.perform(patch("/v1/policy/content/cnt_2").contentType(MediaType.APPLICATION_JSON).content("""
				{
				  "geoAllowList": []
				}
				""")).andExpect(status().isOk()).andExpect(jsonPath("$.data.geoAllowList").isEmpty())
				.andExpect(jsonPath("$.data.geoBlockList[0]").value("US"))
				.andExpect(jsonPath("$.data.ageRestricted").value(false));
	}

	@Test
	void patchRejectsEmptyRequest() throws Exception {
		mockMvc.perform(patch("/v1/policy/content/cnt_3").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void patchRejectsInvalidCountryCode() throws Exception {
		mockMvc.perform(patch("/v1/policy/content/cnt_4").contentType(MediaType.APPLICATION_JSON).content("""
				{
				  "geoAllowList": ["TUR"]
				}
				""")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void patchRejectsOverlappingGeoLists() throws Exception {
		mockMvc.perform(patch("/v1/policy/content/cnt_5").contentType(MediaType.APPLICATION_JSON).content("""
				{
				  "geoAllowList": ["TR"],
				  "geoBlockList": ["TR"]
				}
				""")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("POLICY_GEO_CONFLICT"));
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
