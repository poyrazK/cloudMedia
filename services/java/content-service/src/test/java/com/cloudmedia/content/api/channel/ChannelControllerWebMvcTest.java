package com.cloudmedia.content.api.channel;

import com.cloudmedia.content.persistence.entity.ChannelEntity;
import com.cloudmedia.content.persistence.entity.ChannelMemberEntity;
import com.cloudmedia.content.persistence.entity.ChannelMemberRole;
import com.cloudmedia.content.persistence.repository.ChannelMemberRepository;
import com.cloudmedia.content.persistence.repository.ChannelRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ChannelControllerWebMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ChannelRepository channelRepository;

	@Autowired
	private ChannelMemberRepository channelMemberRepository;

	@Test
	void createChannelReturnsOwnerMembership() throws Exception {
		mockMvc.perform(post("/v1/channels").contentType(MediaType.APPLICATION_JSON)
				.header("X-Request-Id", "req_create_1").content("""
						{
						  "ownerUserId": "user-1",
						  "slug": "tech-channel",
						  "displayName": "Tech Channel",
						  "description": "All about tech"
						}
						""")).andExpect(status().isOk()).andExpect(jsonPath("$.data.channel.id").exists())
				.andExpect(jsonPath("$.data.channel.slug").value("tech-channel"))
				.andExpect(jsonPath("$.data.role").value("OWNER"))
				.andExpect(jsonPath("$.meta.requestId").value("req_create_1"));
	}

	@Test
	void createChannelReturnsValidationErrorForInvalidSlug() throws Exception {
		mockMvc.perform(post("/v1/channels").contentType(MediaType.APPLICATION_JSON).content("""
				{
				  "ownerUserId": "user-1",
				  "slug": "Invalid Slug",
				  "displayName": "Tech Channel"
				}
				""")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.error.details.slug").exists());
	}

	@Test
	void getChannelByIdReturnsChannel() throws Exception {
		ChannelEntity channel = saveChannel("channel-2", "music-channel");

		mockMvc.perform(get("/v1/channels/" + channel.getId()).header("X-Request-Id", "req_get_1"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(channel.getId()))
				.andExpect(jsonPath("$.data.slug").value("music-channel"))
				.andExpect(jsonPath("$.meta.requestId").value("req_get_1"));
	}

	@Test
	void listChannelsByUserReturnsMemberships() throws Exception {
		ChannelEntity first = saveChannel("channel-a", "alpha");
		ChannelEntity second = saveChannel("channel-b", "beta");
		saveMembership(first, "user-1", ChannelMemberRole.OWNER);
		saveMembership(second, "user-1", ChannelMemberRole.ADMIN);

		mockMvc.perform(get("/v1/users/user-1/channels")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].channel.id").value("channel-a"))
				.andExpect(jsonPath("$.data[1].role").value("ADMIN"));
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
}
