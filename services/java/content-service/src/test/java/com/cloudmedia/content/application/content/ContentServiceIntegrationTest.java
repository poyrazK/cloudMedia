package com.cloudmedia.content.application.content;

import com.cloudmedia.content.api.content.dto.CreateContentRequest;
import com.cloudmedia.content.api.content.dto.UpdateContentRequest;
import com.cloudmedia.content.persistence.entity.ChannelEntity;
import com.cloudmedia.content.persistence.entity.ChannelMemberEntity;
import com.cloudmedia.content.persistence.entity.ChannelMemberRole;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class ContentServiceIntegrationTest {

	@Autowired
	private ContentService contentService;

	@Autowired
	private ChannelRepository channelRepository;

	@Autowired
	private ChannelMemberRepository channelMemberRepository;

	@Autowired
	private ContentRepository contentRepository;

	@Test
	void createDraftAppliesDefaultStateAndFlags() {
		ChannelEntity channel = saveChannel("channel-content-1", "content-channel-one");
		saveMembership(channel, "user-1", ChannelMemberRole.OWNER);

		var created = contentService.createDraft(new CreateContentRequest("user-1", "channel-content-1", "Draft title",
				"Draft description", ContentType.VIDEO, null));

		assertEquals(ContentState.DRAFT, created.state());
		assertEquals(ContentVisibility.PRIVATE, created.visibility());
		assertFalse(created.playbackReady());
		assertNull(created.publishedAt());

		var persisted = contentRepository.findById(created.id());
		assertTrue(persisted.isPresent());
		assertEquals(ContentState.DRAFT, persisted.get().getState());
		assertNull(persisted.get().getPublishedAt());
	}

	@Test
	void updateMetadataChangesOnlyProvidedFields() {
		ChannelEntity channel = saveChannel("channel-content-2", "content-channel-two");
		saveMembership(channel, "user-2", ChannelMemberRole.ADMIN);
		var created = contentService.createDraft(new CreateContentRequest("user-2", "channel-content-2",
				"Original title", "Original description", ContentType.VIDEO, ContentVisibility.PRIVATE));

		LocalDateTime previousUpdatedAt = created.updatedAt();
		var updated = contentService.updateMetadata(created.id(),
				new UpdateContentRequest("user-2", "Updated title", null, ContentVisibility.UNLISTED));

		assertEquals("Updated title", updated.title());
		assertEquals("Original description", updated.description());
		assertEquals(ContentVisibility.UNLISTED, updated.visibility());
		assertNotNull(updated.updatedAt());
		assertTrue(updated.updatedAt().isAfter(previousUpdatedAt) || updated.updatedAt().isEqual(previousUpdatedAt));
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
