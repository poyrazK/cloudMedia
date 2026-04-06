package com.cloudmedia.content.application.content;

import com.cloudmedia.content.api.content.dto.CreateContentRequest;
import com.cloudmedia.content.api.content.dto.UpdateContentRequest;
import com.cloudmedia.content.error.ApiException;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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

	@MockBean
	private PolicyEvaluationClient policyEvaluationClient;

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

	@Test
	void getPlaybackReturnsManifestWhenPolicyAllows() {
		ChannelEntity channel = saveChannel("channel-content-3", "content-channel-three");
		ContentEntity content = saveContent(channel, "Playable", "Ready", ContentVisibility.PUBLIC,
				ContentState.PUBLISHED, true);

		when(policyEvaluationClient.evaluate(eq(content.getId()), eq("US"), eq(Boolean.TRUE)))
				.thenReturn(new PolicyDecision(true, List.of()));

		var playback = contentService.getPlayback(content.getId(), "US", true);

		assertEquals(content.getId(), playback.contentId());
		assertTrue(playback.manifestUrl().contains(content.getId()));
		assertEquals(List.of("1080p", "4K"), playback.availableRenditions());
	}

	@Test
	void getPlaybackReturnsForbiddenWhenPolicyDenies() {
		ChannelEntity channel = saveChannel("channel-content-4", "content-channel-four");
		ContentEntity content = saveContent(channel, "Blocked", "Policy blocked", ContentVisibility.PUBLIC,
				ContentState.PUBLISHED, true);

		when(policyEvaluationClient.evaluate(eq(content.getId()), eq("DE"), eq(Boolean.TRUE)))
				.thenReturn(new PolicyDecision(false, List.of("GEO_BLOCKED")));

		ApiException exception = assertThrows(ApiException.class,
				() -> contentService.getPlayback(content.getId(), "DE", true));

		assertEquals("CONTENT_POLICY_DENIED", exception.getCode());
	}

	@Test
	void getPlaybackReturnsConflictForUnplayableState() {
		ChannelEntity channel = saveChannel("channel-content-5", "content-channel-five");
		ContentEntity content = saveContent(channel, "Draft", "Not ready", ContentVisibility.PRIVATE,
				ContentState.DRAFT, false);

		ApiException exception = assertThrows(ApiException.class,
				() -> contentService.getPlayback(content.getId(), "US", true));

		assertEquals("CONTENT_NOT_PLAYABLE", exception.getCode());
	}

	@Test
	void publishTransitionsDraftToPublishedWhenReady() {
		ChannelEntity channel = saveChannel("channel-content-6", "content-channel-six");
		saveMembership(channel, "publisher-1", ChannelMemberRole.OWNER);
		ContentEntity content = saveContent(channel, "Ready draft", "desc", ContentVisibility.PRIVATE,
				ContentState.DRAFT, true);

		var published = contentService.publish(content.getId(), "publisher-1");

		assertEquals(ContentState.PUBLISHED, published.state());
		assertNotNull(published.publishedAt());
	}

	@Test
	void publishRejectsWhenPlaybackNotReady() {
		ChannelEntity channel = saveChannel("channel-content-7", "content-channel-seven");
		saveMembership(channel, "publisher-2", ChannelMemberRole.ADMIN);
		ContentEntity content = saveContent(channel, "Not ready", "desc", ContentVisibility.PRIVATE, ContentState.DRAFT,
				false);

		ApiException exception = assertThrows(ApiException.class,
				() -> contentService.publish(content.getId(), "publisher-2"));

		assertEquals("CONTENT_NOT_READY", exception.getCode());
	}

	@Test
	void publishRejectsWhenStateIsNotDraft() {
		ChannelEntity channel = saveChannel("channel-content-8", "content-channel-eight");
		saveMembership(channel, "publisher-3", ChannelMemberRole.ADMIN);
		ContentEntity content = saveContent(channel, "Already published", "desc", ContentVisibility.PUBLIC,
				ContentState.PUBLISHED, true);

		ApiException exception = assertThrows(ApiException.class,
				() -> contentService.publish(content.getId(), "publisher-3"));

		assertEquals("CONTENT_STATE_INVALID", exception.getCode());
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
