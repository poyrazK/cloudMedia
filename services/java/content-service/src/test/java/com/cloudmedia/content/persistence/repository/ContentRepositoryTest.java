package com.cloudmedia.content.persistence.repository;

import com.cloudmedia.content.persistence.entity.ChannelEntity;
import com.cloudmedia.content.persistence.entity.ChannelMemberEntity;
import com.cloudmedia.content.persistence.entity.ChannelMemberRole;
import com.cloudmedia.content.persistence.entity.ContentEntity;
import com.cloudmedia.content.persistence.entity.ContentState;
import com.cloudmedia.content.persistence.entity.ContentType;
import com.cloudmedia.content.persistence.entity.ContentVisibility;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class ContentRepositoryTest {

	@Autowired
	private ChannelRepository channelRepository;

	@Autowired
	private ChannelMemberRepository channelMemberRepository;

	@Autowired
	private ContentRepository contentRepository;

	@Test
	void channelSlugMustBeUnique() {
		ChannelEntity first = channel("channel-a", "channel-a");
		ChannelEntity second = channel("channel-b", "channel-a");

		channelRepository.saveAndFlush(first);
		assertThrows(DataIntegrityViolationException.class, () -> channelRepository.saveAndFlush(second));
	}

	@Test
	void channelMembershipMustBeUniquePerChannelAndUser() {
		ChannelEntity channel = channelRepository.saveAndFlush(channel("channel-1", "my-channel"));
		ChannelMemberEntity first = member(channel, "user-1", ChannelMemberRole.OWNER);
		ChannelMemberEntity second = member(channel, "user-1", ChannelMemberRole.ADMIN);

		channelMemberRepository.saveAndFlush(first);
		assertThrows(DataIntegrityViolationException.class, () -> channelMemberRepository.saveAndFlush(second));
	}

	@Test
	void findsMembershipsByUserId() {
		ChannelEntity first = channelRepository.saveAndFlush(channel("channel-1", "one"));
		ChannelEntity second = channelRepository.saveAndFlush(channel("channel-2", "two"));

		channelMemberRepository.saveAndFlush(member(first, "user-1", ChannelMemberRole.OWNER));
		channelMemberRepository.saveAndFlush(member(second, "user-1", ChannelMemberRole.ADMIN));
		channelMemberRepository.saveAndFlush(member(second, "user-2", ChannelMemberRole.EDITOR));

		var memberships = channelMemberRepository.findByUserId("user-1");
		assertEquals(2, memberships.size());
	}

	@Test
	void findsContentByChannelAndState() {
		ChannelEntity channel = channelRepository.saveAndFlush(channel("channel-c", "content-channel"));

		contentRepository.saveAndFlush(content(channel, "Draft video", ContentState.DRAFT));
		contentRepository.saveAndFlush(content(channel, "Published video", ContentState.PUBLISHED));

		var draftItems = contentRepository.findByChannel_IdAndState(channel.getId(), ContentState.DRAFT);
		assertEquals(1, draftItems.size());
		assertEquals("Draft video", draftItems.get(0).getTitle());

		var byChannel = contentRepository.findByChannel_Id(channel.getId());
		assertEquals(2, byChannel.size());
	}

	@Test
	void findsContentByIdAndChannelId() {
		ChannelEntity channel = channelRepository.saveAndFlush(channel("channel-d", "lookup-channel"));
		ContentEntity item = contentRepository.saveAndFlush(content(channel, "Lookup video", ContentState.DRAFT));

		var found = contentRepository.findByIdAndChannel_Id(item.getId(), channel.getId());
		assertTrue(found.isPresent());
		assertEquals("Lookup video", found.get().getTitle());
	}

	private ChannelEntity channel(String id, String slug) {
		ChannelEntity channel = new ChannelEntity();
		channel.setId(id);
		channel.setSlug(slug);
		channel.setDisplayName("Display " + slug);
		channel.setDescription("Description " + slug);
		channel.setCreatedAt(LocalDateTime.now());
		channel.setUpdatedAt(LocalDateTime.now());
		return channel;
	}

	private ChannelMemberEntity member(ChannelEntity channel, String userId, ChannelMemberRole role) {
		ChannelMemberEntity member = new ChannelMemberEntity();
		member.setId(UUID.randomUUID().toString());
		member.setChannel(channel);
		member.setUserId(userId);
		member.setRole(role);
		member.setJoinedAt(LocalDateTime.now());
		return member;
	}

	private ContentEntity content(ChannelEntity channel, String title, ContentState state) {
		ContentEntity content = new ContentEntity();
		content.setId(UUID.randomUUID().toString());
		content.setChannel(channel);
		content.setTitle(title);
		content.setDescription("Description for " + title);
		content.setContentType(ContentType.VIDEO);
		content.setState(state);
		content.setVisibility(state == ContentState.PUBLISHED ? ContentVisibility.PUBLIC : ContentVisibility.PRIVATE);
		content.setPlaybackReady(state == ContentState.PUBLISHED);
		content.setCreatedAt(LocalDateTime.now());
		content.setUpdatedAt(LocalDateTime.now());
		content.setPublishedAt(state == ContentState.PUBLISHED ? LocalDateTime.now() : null);
		return content;
	}
}
