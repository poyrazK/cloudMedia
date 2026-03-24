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
class ModerationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ContentPolicyRepository contentPolicyRepository;

	@Test
	void patchCreatesModerationStateWhenMissing() throws Exception {
		mockMvc.perform(patch("/v1/moderation/content/cnt_1").contentType(MediaType.APPLICATION_JSON)
				.header("X-Request-Id", "req_moderation_1").content("""
						{
						  "moderationState": "HIDDEN"
						}
						""")).andExpect(status().isOk()).andExpect(jsonPath("$.data.contentId").value("cnt_1"))
				.andExpect(jsonPath("$.data.moderationState").value("HIDDEN"))
				.andExpect(jsonPath("$.meta.requestId").value("req_moderation_1"));
	}

	@Test
	void patchUpdatesExistingModerationState() throws Exception {
		savePolicy("cnt_2", ModerationState.VISIBLE, "TR", "US");

		mockMvc.perform(patch("/v1/moderation/content/cnt_2").contentType(MediaType.APPLICATION_JSON).content("""
				{
				  "moderationState": "REMOVED"
				}
				""")).andExpect(status().isOk()).andExpect(jsonPath("$.data.moderationState").value("REMOVED"))
				.andExpect(jsonPath("$.data.geoAllowList[0]").value("TR"))
				.andExpect(jsonPath("$.data.geoBlockList[0]").value("US"));
	}

	@Test
	void patchRejectsInvalidModerationState() throws Exception {
		mockMvc.perform(patch("/v1/moderation/content/cnt_3").contentType(MediaType.APPLICATION_JSON).content("""
				{
				  "moderationState": "INVALID"
				}
				""")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	private void savePolicy(String contentId, ModerationState moderationState, String allowList, String blockList) {
		ContentPolicyEntity entity = new ContentPolicyEntity();
		entity.setContentId(contentId);
		entity.setGeoAllowList(allowList);
		entity.setGeoBlockList(blockList);
		entity.setModerationState(moderationState);
		contentPolicyRepository.saveAndFlush(entity);
	}
}
