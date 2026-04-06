package com.cloudmedia.content.api.content;

import com.cloudmedia.content.persistence.entity.ChannelEntity;
import com.cloudmedia.content.persistence.entity.ChannelMemberEntity;
import com.cloudmedia.content.persistence.entity.ChannelMemberRole;
import com.cloudmedia.content.persistence.entity.ContentEntity;
import com.cloudmedia.content.persistence.entity.ContentState;
import com.cloudmedia.content.persistence.entity.ContentType;
import com.cloudmedia.content.persistence.entity.ContentVisibility;
import com.cloudmedia.content.persistence.repository.ChannelMemberRepository;
import com.cloudmedia.content.persistence.repository.ChannelRepository;
import com.cloudmedia.content.persistence.repository.ContentRepository;
import com.cloudmedia.content.policy.PolicyDecision;
import com.cloudmedia.content.policy.PolicyEvaluationClient;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ContentControllerWebMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ChannelRepository channelRepository;

	@Autowired
	private ChannelMemberRepository channelMemberRepository;

	@Autowired
	private ContentRepository contentRepository;

	@MockBean
	private PolicyEvaluationClient policyEvaluationClient;

	@Test
	void createDraftReturnsDraftContent() throws Exception {
		ChannelEntity channel = saveChannel("channel-1", "alpha-channel");
		saveMembership(channel, "user-1", ChannelMemberRole.OWNER);

		mockMvc.perform(post("/v1/content").contentType(MediaType.APPLICATION_JSON)
				.header("X-Request-Id", "req_content_create_1").content("""
						{
						  "userId": "user-1",
						  "channelId": "channel-1",
						  "title": "New Draft",
						  "description": "Initial draft",
						  "contentType": "VIDEO"
						}
						""")).andExpect(status().isOk()).andExpect(jsonPath("$.data.id").exists())
				.andExpect(jsonPath("$.data.channelId").value("channel-1"))
				.andExpect(jsonPath("$.data.state").value("DRAFT"))
				.andExpect(jsonPath("$.data.visibility").value("PRIVATE"))
				.andExpect(jsonPath("$.data.playbackReady").value(false))
				.andExpect(jsonPath("$.meta.requestId").value("req_content_create_1"));
	}

	@Test
	void createDraftReturnsForbiddenForNonMember() throws Exception {
		saveChannel("channel-2", "beta-channel");

		mockMvc.perform(post("/v1/content").contentType(MediaType.APPLICATION_JSON).content("""
				{
				  "userId": "user-2",
				  "channelId": "channel-2",
				  "title": "Forbidden Draft",
				  "contentType": "VIDEO"
				}
				""")).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("CHANNEL_ACCESS_DENIED"));
	}

	@Test
	void updateMetadataReturnsUpdatedContent() throws Exception {
		ChannelEntity channel = saveChannel("channel-3", "gamma-channel");
		saveMembership(channel, "user-3", ChannelMemberRole.ADMIN);
		ContentEntity content = saveContent(channel, "Old title", "Old description", ContentVisibility.PRIVATE,
				ContentState.DRAFT, false);

		mockMvc.perform(patch("/v1/content/" + content.getId()).contentType(MediaType.APPLICATION_JSON).content("""
				{
				  "userId": "user-3",
				  "title": "Updated title",
				  "visibility": "UNLISTED"
				}
				""")).andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(content.getId()))
				.andExpect(jsonPath("$.data.title").value("Updated title"))
				.andExpect(jsonPath("$.data.description").value("Old description"))
				.andExpect(jsonPath("$.data.visibility").value("UNLISTED"));
	}

	@Test
	void updateMetadataReturnsNotFoundForMissingContent() throws Exception {
		mockMvc.perform(patch("/v1/content/missing-id").contentType(MediaType.APPLICATION_JSON).content("""
				{
				  "userId": "user-1",
				  "title": "Updated"
				}
				""")).andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("CONTENT_NOT_FOUND"));
	}

	@Test
	void updateMetadataReturnsForbiddenForNonMember() throws Exception {
		ChannelEntity channel = saveChannel("channel-4", "delta-channel");
		ContentEntity content = saveContent(channel, "Title", "Description", ContentVisibility.PRIVATE,
				ContentState.DRAFT, false);

		mockMvc.perform(patch("/v1/content/" + content.getId()).contentType(MediaType.APPLICATION_JSON).content("""
				{
				  "userId": "user-4",
				  "title": "Updated"
				}
				""")).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("CHANNEL_ACCESS_DENIED"));
	}

	@Test
	void updateMetadataReturnsValidationErrorWhenNoFieldsProvided() throws Exception {
		mockMvc.perform(patch("/v1/content/content-1").contentType(MediaType.APPLICATION_JSON).content("""
				{
				  "userId": "user-1"
				}
				""")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void getPlaybackReturnsPlaybackPayload() throws Exception {
		ChannelEntity channel = saveChannel("channel-5", "epsilon-channel");
		ContentEntity content = saveContent(channel, "Playable title", "Playable description", ContentVisibility.PUBLIC,
				ContentState.PUBLISHED, true);
		when(policyEvaluationClient.evaluate(eq(content.getId()), eq("US"), eq(Boolean.TRUE)))
				.thenReturn(new PolicyDecision(true, List.of()));

		mockMvc.perform(get("/v1/content/{contentId}/playback", content.getId()).param("countryCode", "US")
				.param("ageVerified", "true").header("X-Request-Id", "req_content_playback_1"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.contentId").value(content.getId()))
				.andExpect(jsonPath("$.data.manifestUrl").exists())
				.andExpect(jsonPath("$.data.availableRenditions[0]").value("1080p"))
				.andExpect(jsonPath("$.meta.requestId").value("req_content_playback_1"));
	}

	@Test
	void getPlaybackReturnsForbiddenWhenPolicyDenies() throws Exception {
		ChannelEntity channel = saveChannel("channel-6", "zeta-channel");
		ContentEntity content = saveContent(channel, "Restricted", "Blocked policy", ContentVisibility.PUBLIC,
				ContentState.PUBLISHED, true);
		when(policyEvaluationClient.evaluate(eq(content.getId()), eq("DE"), eq(Boolean.TRUE)))
				.thenReturn(new PolicyDecision(false, List.of("GEO_BLOCKED")));

		mockMvc.perform(get("/v1/content/{contentId}/playback", content.getId()).param("countryCode", "DE")
				.param("ageVerified", "true")).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("CONTENT_POLICY_DENIED"));
	}

	@Test
	void getPlaybackReturnsConflictWhenContentNotReady() throws Exception {
		ChannelEntity channel = saveChannel("channel-7", "eta-channel");
		ContentEntity content = saveContent(channel, "Draft", "Not ready", ContentVisibility.PRIVATE,
				ContentState.DRAFT, false);

		mockMvc.perform(get("/v1/content/{contentId}/playback", content.getId())).andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("CONTENT_NOT_PLAYABLE"));
	}

	@Test
	void publishReturnsPublishedContent() throws Exception {
		ChannelEntity channel = saveChannel("channel-8", "theta-channel");
		saveMembership(channel, "user-8", ChannelMemberRole.ADMIN);
		ContentEntity content = saveContent(channel, "Draft", "Ready", ContentVisibility.PRIVATE, ContentState.DRAFT,
				true);

		mockMvc.perform(post("/v1/content/{contentId}/publish", content.getId()).contentType(MediaType.APPLICATION_JSON)
				.header("X-Request-Id", "req_content_publish_1").content("""
						{
						  "userId": "user-8"
						}
						""")).andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(content.getId()))
				.andExpect(jsonPath("$.data.state").value("PUBLISHED"))
				.andExpect(jsonPath("$.data.publishedAt").exists())
				.andExpect(jsonPath("$.meta.requestId").value("req_content_publish_1"));
	}

	@Test
	void publishReturnsConflictWhenNotPlaybackReady() throws Exception {
		ChannelEntity channel = saveChannel("channel-9", "iota-channel");
		saveMembership(channel, "user-9", ChannelMemberRole.OWNER);
		ContentEntity content = saveContent(channel, "Draft", "Not ready", ContentVisibility.PRIVATE,
				ContentState.DRAFT, false);

		mockMvc.perform(post("/v1/content/{contentId}/publish", content.getId()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "userId": "user-9"
						}
						""")).andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("CONTENT_NOT_READY"));
	}

	@Test
	void publishReturnsForbiddenForNonMember() throws Exception {
		ChannelEntity channel = saveChannel("channel-10", "kappa-channel");
		ContentEntity content = saveContent(channel, "Draft", "Ready", ContentVisibility.PRIVATE, ContentState.DRAFT,
				true);

		mockMvc.perform(post("/v1/content/{contentId}/publish", content.getId()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "userId": "user-10"
						}
						""")).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("CHANNEL_ACCESS_DENIED"));
	}

	@Test
	void unpublishReturnsPrivateContent() throws Exception {
		ChannelEntity channel = saveChannel("channel-11", "lambda-channel");
		saveMembership(channel, "user-11", ChannelMemberRole.ADMIN);
		ContentEntity content = saveContent(channel, "Published", "Ready", ContentVisibility.PUBLIC,
				ContentState.PUBLISHED, true);

		mockMvc.perform(post("/v1/content/{contentId}/unpublish", content.getId())
				.contentType(MediaType.APPLICATION_JSON).header("X-Request-Id", "req_content_unpublish_1").content("""
						{
						  "userId": "user-11"
						}
						""")).andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(content.getId()))
				.andExpect(jsonPath("$.data.state").value("PRIVATE"))
				.andExpect(jsonPath("$.meta.requestId").value("req_content_unpublish_1"));
	}

	@Test
	void unpublishReturnsConflictWhenStateIsNotPublished() throws Exception {
		ChannelEntity channel = saveChannel("channel-12", "mu-channel");
		saveMembership(channel, "user-12", ChannelMemberRole.OWNER);
		ContentEntity content = saveContent(channel, "Draft", "Not published", ContentVisibility.PRIVATE,
				ContentState.DRAFT, true);

		mockMvc.perform(post("/v1/content/{contentId}/unpublish", content.getId())
				.contentType(MediaType.APPLICATION_JSON).content("""
						{
						  "userId": "user-12"
						}
						""")).andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("CONTENT_STATE_INVALID"));
	}

	@Test
	void unpublishReturnsForbiddenForNonMember() throws Exception {
		ChannelEntity channel = saveChannel("channel-13", "nu-channel");
		ContentEntity content = saveContent(channel, "Published", "Ready", ContentVisibility.PUBLIC,
				ContentState.PUBLISHED, true);

		mockMvc.perform(post("/v1/content/{contentId}/unpublish", content.getId())
				.contentType(MediaType.APPLICATION_JSON).content("""
						{
						  "userId": "user-13"
						}
						""")).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("CHANNEL_ACCESS_DENIED"));
	}

	private ChannelEntity saveChannel(String id, String slug) {
		ChannelEntity channel = new ChannelEntity();
		channel.setId(id);
		channel.setSlug(slug);
		channel.setDisplayName("Display " + slug);
		channel.setDescription("Desc " + slug);
		channel.setCreatedAt(LocalDateTime.now());
		channel.setUpdatedAt(LocalDateTime.now());
		return channelRepository.saveAndFlush(channel);
	}

	private void saveMembership(ChannelEntity channel, String userId, ChannelMemberRole role) {
		ChannelMemberEntity member = new ChannelMemberEntity();
		member.setId(UUID.randomUUID().toString());
		member.setChannel(channel);
		member.setUserId(userId);
		member.setRole(role);
		member.setJoinedAt(LocalDateTime.now());
		channelMemberRepository.saveAndFlush(member);
	}

	private ContentEntity saveContent(ChannelEntity channel, String title, String description,
			ContentVisibility visibility, ContentState state, boolean playbackReady) {
		ContentEntity content = new ContentEntity();
		content.setId(UUID.randomUUID().toString());
		content.setChannel(channel);
		content.setTitle(title);
		content.setDescription(description);
		content.setContentType(ContentType.VIDEO);
		content.setState(state);
		content.setVisibility(visibility);
		content.setPlaybackReady(playbackReady);
		content.setCreatedAt(LocalDateTime.now());
		content.setUpdatedAt(LocalDateTime.now());
		content.setPublishedAt(state == ContentState.PUBLISHED ? LocalDateTime.now() : null);
		return contentRepository.saveAndFlush(content);
	}
}
