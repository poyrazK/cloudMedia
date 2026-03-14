package com.cloudmedia.content.application.content;

import com.cloudmedia.content.api.content.dto.CreateContentRequest;
import com.cloudmedia.content.api.content.dto.UpdateContentRequest;
import com.cloudmedia.content.events.ContentCreatedPayload;
import com.cloudmedia.content.events.ContentEventEnvelope;
import com.cloudmedia.content.events.ContentEventPublisher;
import com.cloudmedia.content.events.ContentPublishedPayload;
import com.cloudmedia.content.events.ContentUnpublishedPayload;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

	@Autowired
	private RecordingContentEventPublisher contentEventPublisher;

	@Test
	void createDraftAppliesDefaultStateAndFlags() {
		contentEventPublisher.clear();
		ChannelEntity channel = saveChannel("channel-content-1", "content-channel-one");
		saveMembership(channel, "user-1", ChannelMemberRole.OWNER);

		var created = contentService.createDraft(new CreateContentRequest("user-1", "channel-content-1", "Draft title",
				"Draft description", ContentType.VIDEO, null), "req_content_created_1");

		assertEquals(ContentState.DRAFT, created.state());
		assertEquals(ContentVisibility.PRIVATE, created.visibility());
		assertFalse(created.playbackReady());
		assertNull(created.publishedAt());

		var persisted = contentRepository.findById(created.id());
		assertTrue(persisted.isPresent());
		assertEquals(ContentState.DRAFT, persisted.get().getState());
		assertNull(persisted.get().getPublishedAt());

		ContentEventEnvelope event = contentEventPublisher.singleEvent();
		assertEquals("content.created", event.eventType());
		assertEquals("req_content_created_1", event.traceId());
		ContentCreatedPayload payload = (ContentCreatedPayload) event.payload();
		assertEquals(created.id(), payload.contentId());
		assertEquals("channel-content-1", payload.channelId());
	}

	@Test
	void updateMetadataChangesOnlyProvidedFields() {
		contentEventPublisher.clear();
		ChannelEntity channel = saveChannel("channel-content-2", "content-channel-two");
		saveMembership(channel, "user-2", ChannelMemberRole.ADMIN);
		var created = contentService.createDraft(new CreateContentRequest("user-2", "channel-content-2",
				"Original title", "Original description", ContentType.VIDEO, ContentVisibility.PRIVATE));
		contentEventPublisher.clear();

		LocalDateTime previousUpdatedAt = created.updatedAt();
		var updated = contentService.updateMetadata(created.id(),
				new UpdateContentRequest("user-2", "Updated title", null, ContentVisibility.UNLISTED),
				"req_content_updated_1");

		assertEquals("Updated title", updated.title());
		assertEquals("Original description", updated.description());
		assertEquals(ContentVisibility.UNLISTED, updated.visibility());
		assertNotNull(updated.updatedAt());
		assertTrue(updated.updatedAt().isAfter(previousUpdatedAt) || updated.updatedAt().isEqual(previousUpdatedAt));

		ContentEventEnvelope event = contentEventPublisher.singleEvent();
		assertEquals("content.updated", event.eventType());
		assertEquals("req_content_updated_1", event.traceId());
		ContentUpdatedPayload payload = (ContentUpdatedPayload) event.payload();
		assertEquals(created.id(), payload.contentId());
		assertEquals("Updated title", payload.title());
	}

	@Test
	void publishUpdatesLifecycleWhenPlaybackReady() {
		contentEventPublisher.clear();
		ChannelEntity channel = saveChannel("channel-content-3", "content-channel-three");
		saveMembership(channel, "user-3", ChannelMemberRole.OWNER);
		var created = contentService.createDraft(new CreateContentRequest("user-3", "channel-content-3", "Ready title",
				"Ready description", ContentType.VIDEO, ContentVisibility.PUBLIC));

		ContentEntity entity = contentRepository.findById(created.id()).orElseThrow();
		entity.setPlaybackReady(true);
		contentRepository.saveAndFlush(entity);
		contentEventPublisher.clear();

		var published = contentService.publish(created.id(), "user-3", "req_content_published_1");

		assertEquals(ContentState.PUBLISHED, published.state());
		assertNotNull(published.publishedAt());
		assertEquals(ContentVisibility.PUBLIC, published.visibility());

		ContentEventEnvelope event = contentEventPublisher.singleEvent();
		assertEquals("content.published", event.eventType());
		assertEquals("req_content_published_1", event.traceId());
		ContentPublishedPayload payload = (ContentPublishedPayload) event.payload();
		assertEquals(created.id(), payload.contentId());
		assertEquals(published.publishedAt(), payload.publishedAt());
	}

	@Test
	void publishFailsWhenPlaybackNotReady() {
		ChannelEntity channel = saveChannel("channel-content-4", "content-channel-four");
		saveMembership(channel, "user-4", ChannelMemberRole.OWNER);
		var created = contentService.createDraft(new CreateContentRequest("user-4", "channel-content-4", "Not ready",
				"Draft", ContentType.VIDEO, ContentVisibility.PRIVATE));

		ApiException exception = assertThrows(ApiException.class, () -> contentService.publish(created.id(), "user-4"));
		assertEquals("CONTENT_NOT_READY_FOR_PUBLISH", exception.getCode());
	}

	@Test
	void unpublishMovesPublishedContentToPrivateState() {
		contentEventPublisher.clear();
		ChannelEntity channel = saveChannel("channel-content-5", "content-channel-five");
		saveMembership(channel, "user-5", ChannelMemberRole.ADMIN);
		var created = contentService.createDraft(new CreateContentRequest("user-5", "channel-content-5", "Ready",
				"Desc", ContentType.VIDEO, ContentVisibility.UNLISTED));

		ContentEntity entity = contentRepository.findById(created.id()).orElseThrow();
		entity.setPlaybackReady(true);
		entity.setState(ContentState.PUBLISHED);
		entity.setPublishedAt(LocalDateTime.now().minusHours(1));
		contentRepository.saveAndFlush(entity);
		contentEventPublisher.clear();

		var unpublished = contentService.unpublish(created.id(), "user-5", "req_content_unpublished_1");

		assertEquals(ContentState.PRIVATE, unpublished.state());
		assertNotNull(unpublished.publishedAt());
		assertEquals(ContentVisibility.UNLISTED, unpublished.visibility());

		ContentEventEnvelope event = contentEventPublisher.singleEvent();
		assertEquals("content.unpublished", event.eventType());
		assertEquals("req_content_unpublished_1", event.traceId());
		ContentUnpublishedPayload payload = (ContentUnpublishedPayload) event.payload();
		assertEquals(ContentState.PUBLISHED, payload.previousState());
		assertEquals(ContentState.PRIVATE, payload.currentState());
	}

	@Test
	void getPlaybackReturnsManifestAndRenditionsForPublishedReadyContent() {
		ChannelEntity channel = saveChannel("channel-content-6", "content-channel-six");
		saveMembership(channel, "user-6", ChannelMemberRole.OWNER);
		var created = contentService.createDraft(new CreateContentRequest("user-6", "channel-content-6", "Playable",
				"Desc", ContentType.VIDEO, ContentVisibility.PUBLIC));

		ContentEntity entity = contentRepository.findById(created.id()).orElseThrow();
		entity.setPlaybackReady(true);
		entity.setState(ContentState.PUBLISHED);
		entity.setPublishedAt(LocalDateTime.now().minusMinutes(10));
		contentRepository.saveAndFlush(entity);

		var playback = contentService.getPlayback(created.id());

		assertEquals(created.id(), playback.contentId());
		assertEquals("https://playback.cloudmedia.local/content/" + created.id() + "/master.m3u8",
				playback.manifestUrl());
		assertTrue(playback.playbackReady());
		assertEquals(2, playback.availableRenditions().size());
	}

	@Test
	void getPlaybackRejectsDraftContent() {
		ChannelEntity channel = saveChannel("channel-content-7", "content-channel-seven");
		saveMembership(channel, "user-7", ChannelMemberRole.OWNER);
		var created = contentService.createDraft(new CreateContentRequest("user-7", "channel-content-7", "Draft",
				"Desc", ContentType.VIDEO, ContentVisibility.PRIVATE));

		ApiException exception = assertThrows(ApiException.class, () -> contentService.getPlayback(created.id()));
		assertEquals("CONTENT_NOT_PLAYABLE", exception.getCode());
	}

	@Test
	void getPlaybackRejectsPublishedContentThatIsNotReady() {
		ChannelEntity channel = saveChannel("channel-content-8", "content-channel-eight");
		saveMembership(channel, "user-8", ChannelMemberRole.OWNER);
		var created = contentService.createDraft(new CreateContentRequest("user-8", "channel-content-8", "Pending",
				"Desc", ContentType.VIDEO, ContentVisibility.PUBLIC));

		ContentEntity entity = contentRepository.findById(created.id()).orElseThrow();
		entity.setState(ContentState.PUBLISHED);
		entity.setPublishedAt(LocalDateTime.now().minusMinutes(5));
		contentRepository.saveAndFlush(entity);

		ApiException exception = assertThrows(ApiException.class, () -> contentService.getPlayback(created.id()));
		assertEquals("CONTENT_NOT_PLAYBACK_READY", exception.getCode());
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

	@TestConfiguration
	static class TestEventPublisherConfiguration {

		@Bean
		@Primary
		RecordingContentEventPublisher recordingContentEventPublisher() {
			return new RecordingContentEventPublisher();
		}
	}

	static class RecordingContentEventPublisher implements ContentEventPublisher {

		private final List<ContentEventEnvelope> events = new ArrayList<>();

		@Override
		public void publish(ContentEventEnvelope event) {
			events.add(event);
		}

		void clear() {
			events.clear();
		}

		ContentEventEnvelope singleEvent() {
			return events.getFirst();
		}
	}
}
