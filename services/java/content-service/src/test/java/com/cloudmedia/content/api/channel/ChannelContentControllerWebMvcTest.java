package com.cloudmedia.content.api.channel;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ChannelContentControllerWebMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ChannelRepository channelRepository;

	@Autowired
	private ChannelMemberRepository channelMemberRepository;

	@Autowired
	private ContentRepository contentRepository;

	@Test
	void listChannelContentReturnsEmptyListForChannelWithNoContent() throws Exception {
		ChannelEntity channel = saveChannel("channel-empty", "empty-channel");

		mockMvc.perform(get("/v1/channels/" + channel.getId() + "/content").header("X-Request-Id", "req_list_1"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray())
				.andExpect(jsonPath("$.data").isEmpty()).andExpect(jsonPath("$.meta.requestId").value("req_list_1"));
	}

	@Test
	void listChannelContentReturnsContentForChannel() throws Exception {
		ChannelEntity channel = saveChannel("channel-1", "test-channel");
		saveMembership(channel, "user-1", ChannelMemberRole.OWNER);
		saveContent(channel, "content-1", "Test Video 1", ContentState.PUBLISHED);
		saveContent(channel, "content-2", "Test Video 2", ContentState.DRAFT);

		mockMvc.perform(get("/v1/channels/" + channel.getId() + "/content").header("X-Request-Id", "req_list_2"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray())
				.andExpect(jsonPath("$.data.length()").value(2))
				.andExpect(jsonPath("$.data[0].title").value("Test Video 1"))
				.andExpect(jsonPath("$.data[1].title").value("Test Video 2"));
	}

	@Test
	void listChannelContentFiltersByState() throws Exception {
		ChannelEntity channel = saveChannel("channel-2", "filter-channel");
		saveMembership(channel, "user-1", ChannelMemberRole.OWNER);
		saveContent(channel, "content-draft", "Draft Video", ContentState.DRAFT);
		saveContent(channel, "content-published", "Published Video", ContentState.PUBLISHED);

		mockMvc.perform(get("/v1/channels/" + channel.getId() + "/content").param("state", "PUBLISHED")
				.header("X-Request-Id", "req_list_3")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data").isArray()).andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].state").value("PUBLISHED"));
	}

	@Test
	void listChannelContentReturns404ForNonexistentChannel() throws Exception {
		mockMvc.perform(get("/v1/channels/nonexistent-channel/content").header("X-Request-Id", "req_list_4"))
				.andExpect(status().isNotFound());
	}

	@Test
	void listChannelContentReturnsContentWithThumbnailUrl() throws Exception {
		ChannelEntity channel = saveChannel("channel-thumb", "thumb-channel");
		ContentEntity content = saveContent(channel, "content-thumb", "video-thumb", ContentState.PUBLISHED);
		content.setThumbnailUrl("https://cdn.example.com/thumb/xyz.jpg");
		contentRepository.saveAndFlush(content);

		mockMvc.perform(get("/v1/channels/" + channel.getId() + "/content").header("X-Request-Id", "req_thumb"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].thumbnailUrl").value("https://cdn.example.com/thumb/xyz.jpg"));
	}

	@Test
	void listChannelContentReturnsContentOrderedByCreatedAt() throws Exception {
		ChannelEntity channel = saveChannel("channel-order", "order-channel");
		saveMembership(channel, "user-order", ChannelMemberRole.OWNER);
		ContentEntity first = saveContent(channel, "content-order-first", "First Video", ContentState.PUBLISHED);
		first.setCreatedAt(LocalDateTime.now().minusDays(1));
		contentRepository.saveAndFlush(first);
		ContentEntity second = saveContent(channel, "content-order-second", "Second Video", ContentState.PUBLISHED);
		second.setCreatedAt(LocalDateTime.now());
		contentRepository.saveAndFlush(second);

		mockMvc.perform(get("/v1/channels/" + channel.getId() + "/content").header("X-Request-Id", "req_order"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data[0].title").value("First Video"))
				.andExpect(jsonPath("$.data[1].title").value("Second Video"));
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

	private ContentEntity saveContent(ChannelEntity channel, String id, String title, ContentState state) {
		ContentEntity content = new ContentEntity();
		content.setId(id);
		content.setChannel(channel);
		content.setTitle(title);
		content.setDescription("Description for " + title);
		content.setContentType(ContentType.VIDEO);
		content.setState(state);
		content.setVisibility(ContentVisibility.PUBLIC);
		content.setPlaybackReady(true);
		content.setCreatedAt(LocalDateTime.now());
		content.setUpdatedAt(LocalDateTime.now());
		if (state == ContentState.PUBLISHED) {
			content.setPublishedAt(LocalDateTime.now());
		}
		return contentRepository.saveAndFlush(content);
	}
}
