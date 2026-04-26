package com.cloudmedia.content.application.content;

import com.cloudmedia.content.api.content.dto.CreateContentRequest;
import com.cloudmedia.content.api.content.dto.UpdateContentRequest;
import com.cloudmedia.content.events.ContentEventPublisher;
import com.cloudmedia.content.events.ContentPublishedPayload;
import com.cloudmedia.content.events.ContentUpdatedPayload;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

	@MockBean
	private ContentEventPublisher contentEventPublisher;

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
				new UpdateContentRequest("user-2", "Updated title", null, ContentVisibility.UNLISTED, null));

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
		verify(contentEventPublisher).publishContentPublished(
				eq(new ContentPublishedPayload(content.getId(), channel.getId(), "Ready draft", "desc",
						ContentType.VIDEO.name(), ContentVisibility.PRIVATE.name(), published.publishedAt())),
				isNull());
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
		verify(contentEventPublisher, never()).publishContentPublished(any(), any());
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
		verify(contentEventPublisher, never()).publishContentPublished(any(), any());
	}

	@Test
	void unpublishTransitionsPublishedToPrivate() {
		ChannelEntity channel = saveChannel("channel-content-9", "content-channel-nine");
		saveMembership(channel, "publisher-4", ChannelMemberRole.OWNER);
		ContentEntity content = saveContent(channel, "Published", "desc", ContentVisibility.PUBLIC,
				ContentState.PUBLISHED, true);
		LocalDateTime publishedAtBefore = content.getPublishedAt();

		var unpublished = contentService.unpublish(content.getId(), "publisher-4");

		assertEquals(ContentState.PRIVATE, unpublished.state());
		assertEquals(publishedAtBefore, unpublished.publishedAt());
	}

	@Test
	void unpublishRejectsWhenStateIsNotPublished() {
		ChannelEntity channel = saveChannel("channel-content-10", "content-channel-ten");
		saveMembership(channel, "publisher-5", ChannelMemberRole.ADMIN);
		ContentEntity content = saveContent(channel, "Draft", "desc", ContentVisibility.PRIVATE, ContentState.DRAFT,
				true);

		ApiException exception = assertThrows(ApiException.class,
				() -> contentService.unpublish(content.getId(), "publisher-5"));

		assertEquals("CONTENT_STATE_INVALID", exception.getCode());
	}

	@Test
	void updateMetadataEmitsContentUpdatedWhenPublished() {
		ChannelEntity channel = saveChannel("channel-content-11", "content-channel-eleven");
		saveMembership(channel, "updater-1", ChannelMemberRole.ADMIN);
		ContentEntity content = saveContent(channel, "Original", "desc", ContentVisibility.PUBLIC,
				ContentState.PUBLISHED, true);

		contentService.updateMetadata(content.getId(),
				new UpdateContentRequest("updater-1", "Updated Title", null, ContentVisibility.UNLISTED, null));

		verify(contentEventPublisher).publishContentUpdated(eq(new ContentUpdatedPayload(content.getId(),
				channel.getId(), "Updated Title", ContentType.VIDEO.name(), ContentVisibility.UNLISTED.name())),
				isNull());
	}

	@Test
	void listByChannelReturnsEmptyListWhenNoPublicContent() {
		ChannelEntity channel = saveChannel("channel-list-vis-1", "list-channel-vis-one");

		var result = contentService.listByChannel(channel.getId(), null);

		assertTrue(result.isEmpty());
	}

	@Test
	void listByChannelFiltersPrivateContent() {
		ChannelEntity channel = saveChannel("channel-list-vis-2", "list-channel-vis-two");
		saveMembership(channel, "user-vis-1", ChannelMemberRole.OWNER);
		saveContent(channel, "Private Video", "desc", ContentVisibility.PRIVATE, ContentState.PUBLISHED, true);
		saveContent(channel, "Public Video", "desc", ContentVisibility.PUBLIC, ContentState.PUBLISHED, true);

		var result = contentService.listByChannel(channel.getId(), null);

		assertEquals(1, result.size());
		assertEquals("Public Video", result.get(0).title());
	}

	@Test
	void listByChannelFiltersByStateAndVisibility() {
		ChannelEntity channel = saveChannel("channel-list-vis-3", "list-channel-vis-three");
		saveMembership(channel, "user-vis-2", ChannelMemberRole.OWNER);
		saveContent(channel, "Private Draft", "desc", ContentVisibility.PRIVATE, ContentState.DRAFT, false);
		saveContent(channel, "Public Draft", "desc", ContentVisibility.PUBLIC, ContentState.DRAFT, false);
		saveContent(channel, "Private Published", "desc", ContentVisibility.PRIVATE, ContentState.PUBLISHED, true);
		saveContent(channel, "Public Published", "desc", ContentVisibility.PUBLIC, ContentState.PUBLISHED, true);

		var result = contentService.listByChannel(channel.getId(), ContentState.PUBLISHED);

		assertEquals(1, result.size());
		assertEquals("Public Published", result.get(0).title());
	}

	@Test
	void listByChannelReturnsContentOrderedByCreatedAt() {
		ChannelEntity channel = saveChannel("channel-list-vis-4", "list-channel-vis-four");
		saveMembership(channel, "user-vis-3", ChannelMemberRole.OWNER);
		ContentEntity first = saveContent(channel, "First", "desc", ContentVisibility.PUBLIC, ContentState.PUBLISHED,
				true);
		first.setCreatedAt(LocalDateTime.now().minusDays(1));
		contentRepository.saveAndFlush(first);
		ContentEntity second = saveContent(channel, "Second", "desc", ContentVisibility.PUBLIC, ContentState.PUBLISHED,
				true);
		second.setCreatedAt(LocalDateTime.now());
		contentRepository.saveAndFlush(second);

		var result = contentService.listByChannel(channel.getId(), null);

		assertEquals(2, result.size());
		assertEquals("First", result.get(0).title());
		assertEquals("Second", result.get(1).title());
	}

	@Test
	void updateMetadataSetsThumbnailUrl() {
		ChannelEntity channel = saveChannel("channel-vis-thumb", "vis-thumb-channel");
		saveMembership(channel, "user-thumb", ChannelMemberRole.OWNER);
		ContentEntity content = saveContent(channel, "Video", "desc", ContentVisibility.PUBLIC, ContentState.DRAFT,
				false);

		var updated = contentService.updateMetadata(content.getId(),
				new UpdateContentRequest("user-thumb", null, null, null, "https://cdn.example.com/thumb/xyz.jpg"));

		assertEquals("https://cdn.example.com/thumb/xyz.jpg", updated.thumbnailUrl());
	}

	@Test
	void listByChannelReturns404WhenChannelNotFound() {
		ApiException exception = assertThrows(ApiException.class,
				() -> contentService.listByChannel("nonexistent-channel", null));

		assertEquals("CHANNEL_NOT_FOUND", exception.getCode());
	}

	@Test
	void listByChannelIncludesThumbnailUrl() {
		ChannelEntity channel = saveChannel("channel-list-4", "list-channel-four");
		ContentEntity content = saveContent(channel, "With Thumbnail", "desc", ContentVisibility.PUBLIC,
				ContentState.PUBLISHED, true);
		content.setThumbnailUrl("https://cdn.example.com/thumb/abc123.jpg");
		contentRepository.saveAndFlush(content);

		var result = contentService.listByChannel(channel.getId(), null);

		assertEquals(1, result.size());
		assertEquals("https://cdn.example.com/thumb/abc123.jpg", result.get(0).thumbnailUrl());
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
