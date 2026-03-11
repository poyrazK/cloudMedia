package com.cloudmedia.content.application.channel;

import com.cloudmedia.content.api.channel.dto.CreateChannelRequest;
import com.cloudmedia.content.persistence.entity.ChannelMemberRole;
import com.cloudmedia.content.persistence.repository.ChannelMemberRepository;
import com.cloudmedia.content.persistence.repository.ChannelRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class ChannelServiceIntegrationTest {

	@Autowired
	private ChannelService channelService;

	@Autowired
	private ChannelRepository channelRepository;

	@Autowired
	private ChannelMemberRepository channelMemberRepository;

	@Test
	void createChannelCreatesOwnerMembership() {
		CreateChannelRequest request = new CreateChannelRequest("user-1", "travel-channel", "Travel Channel",
				"Travel videos");

		var created = channelService.createChannel(request);

		assertEquals("travel-channel", created.channel().slug());
		assertEquals(ChannelMemberRole.OWNER, created.role());
		assertTrue(channelRepository.findBySlug("travel-channel").isPresent());

		var membership = channelMemberRepository.findByChannel_IdAndUserId(created.channel().id(), "user-1");
		assertTrue(membership.isPresent());
		assertEquals(ChannelMemberRole.OWNER, membership.get().getRole());
	}

	@Test
	void listByUserIdReturnsMultipleChannelsForSameUser() {
		channelService.createChannel(new CreateChannelRequest("user-1", "tech-channel", "Tech", "Tech content"));
		channelService.createChannel(new CreateChannelRequest("user-1", "music-channel", "Music", "Music content"));

		var channels = channelService.listByUserId("user-1");
		assertEquals(2, channels.size());
	}
}
