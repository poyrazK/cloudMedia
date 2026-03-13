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
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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
				ContentState.DRAFT, false, null);

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
				ContentState.DRAFT, false, null);

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
	void publishReturnsPublishedContentWhenReady() throws Exception {
		ChannelEntity channel = saveChannel("channel-5", "epsilon-channel");
		saveMembership(channel, "user-5", ChannelMemberRole.OWNER);
		ContentEntity content = saveContent(channel, "Publish me", "Ready", ContentVisibility.PUBLIC,
				ContentState.DRAFT, true, null);

		mockMvc.perform(
				post("/v1/content/" + content.getId() + "/publish").contentType(MediaType.APPLICATION_JSON).content("""
						{
						  "userId": "user-5"
						}
						""")).andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(content.getId()))
				.andExpect(jsonPath("$.data.state").value("PUBLISHED"))
				.andExpect(jsonPath("$.data.publishedAt").exists());
	}

	@Test
	void publishReturnsConflictWhenNotPlaybackReady() throws Exception {
		ChannelEntity channel = saveChannel("channel-6", "zeta-channel");
		saveMembership(channel, "user-6", ChannelMemberRole.OWNER);
		ContentEntity content = saveContent(channel, "Not ready", "Wait", ContentVisibility.PRIVATE, ContentState.DRAFT,
				false, null);

		mockMvc.perform(
				post("/v1/content/" + content.getId() + "/publish").contentType(MediaType.APPLICATION_JSON).content("""
						{
						  "userId": "user-6"
						}
						""")).andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("CONTENT_NOT_READY_FOR_PUBLISH"));
	}

	@Test
	void unpublishReturnsPrivateStateForPublishedContent() throws Exception {
		ChannelEntity channel = saveChannel("channel-7", "eta-channel");
		saveMembership(channel, "user-7", ChannelMemberRole.ADMIN);
		ContentEntity content = saveContent(channel, "Published", "Visible", ContentVisibility.PUBLIC,
				ContentState.PUBLISHED, true, LocalDateTime.now().minusDays(1));

		mockMvc.perform(post("/v1/content/" + content.getId() + "/unpublish").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "userId": "user-7"
						}
						""")).andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(content.getId()))
				.andExpect(jsonPath("$.data.state").value("PRIVATE"));
	}

	@Test
	void unpublishReturnsConflictWhenNotPublished() throws Exception {
		ChannelEntity channel = saveChannel("channel-8", "theta-channel");
		saveMembership(channel, "user-8", ChannelMemberRole.ADMIN);
		ContentEntity content = saveContent(channel, "Draft", "Not public", ContentVisibility.PRIVATE,
				ContentState.DRAFT, false, null);

		mockMvc.perform(post("/v1/content/" + content.getId() + "/unpublish").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "userId": "user-8"
						}
						""")).andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("CONTENT_NOT_PUBLISHED"));
	}

	@Test
	void getPlaybackReturnsManifestForPublishedReadyContent() throws Exception {
		ChannelEntity channel = saveChannel("channel-9", "iota-channel");
		ContentEntity content = saveContent(channel, "Playable", "Ready to stream", ContentVisibility.PUBLIC,
				ContentState.PUBLISHED, true, LocalDateTime.now().minusMinutes(5));

		mockMvc.perform(get("/v1/content/" + content.getId() + "/playback").header("X-Request-Id", "req_playback_1"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.contentId").value(content.getId()))
				.andExpect(jsonPath("$.data.manifestUrl")
						.value("https://playback.cloudmedia.local/content/" + content.getId() + "/master.m3u8"))
				.andExpect(jsonPath("$.data.playbackReady").value(true))
				.andExpect(jsonPath("$.data.availableRenditions[0].name").value("sd"))
				.andExpect(jsonPath("$.meta.requestId").value("req_playback_1"));
	}

	@Test
	void getPlaybackRejectsNonPublishedContent() throws Exception {
		ChannelEntity channel = saveChannel("channel-10", "kappa-channel");
		ContentEntity content = saveContent(channel, "Draft", "Not public", ContentVisibility.PRIVATE,
				ContentState.DRAFT, true, null);

		mockMvc.perform(get("/v1/content/" + content.getId() + "/playback")).andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("CONTENT_NOT_PLAYABLE"));
	}

	@Test
	void getPlaybackRejectsPublishedContentThatIsNotReady() throws Exception {
		ChannelEntity channel = saveChannel("channel-11", "lambda-channel");
		ContentEntity content = saveContent(channel, "Pending", "Not ready", ContentVisibility.PUBLIC,
				ContentState.PUBLISHED, false, LocalDateTime.now().minusMinutes(1));

		mockMvc.perform(get("/v1/content/" + content.getId() + "/playback")).andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("CONTENT_NOT_PLAYBACK_READY"));
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
			ContentVisibility visibility, ContentState state, boolean playbackReady, LocalDateTime publishedAt) {
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
		content.setPublishedAt(publishedAt);
		return contentRepository.saveAndFlush(content);
	}
}
