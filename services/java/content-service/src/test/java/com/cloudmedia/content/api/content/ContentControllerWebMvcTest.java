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
		ContentEntity content = saveContent(channel, "Old title", "Old description", ContentVisibility.PRIVATE);

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
		ContentEntity content = saveContent(channel, "Title", "Description", ContentVisibility.PRIVATE);

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
			ContentVisibility visibility) {
		ContentEntity content = new ContentEntity();
		content.setId(UUID.randomUUID().toString());
		content.setChannel(channel);
		content.setTitle(title);
		content.setDescription(description);
		content.setContentType(ContentType.VIDEO);
		content.setState(ContentState.DRAFT);
		content.setVisibility(visibility);
		content.setPlaybackReady(false);
		content.setCreatedAt(LocalDateTime.now());
		content.setUpdatedAt(LocalDateTime.now());
		content.setPublishedAt(null);
		return contentRepository.saveAndFlush(content);
	}
}
