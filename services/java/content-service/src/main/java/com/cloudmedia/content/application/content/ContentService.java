package com.cloudmedia.content.application.content;

import com.cloudmedia.content.api.content.dto.ContentResponse;
import com.cloudmedia.content.api.content.dto.PlaybackResponse;
import com.cloudmedia.content.api.content.dto.PlaybackRenditionResponse;
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
import com.cloudmedia.content.persistence.entity.ContentEntity;
import com.cloudmedia.content.persistence.entity.ContentState;
import com.cloudmedia.content.persistence.entity.ContentVisibility;
import com.cloudmedia.content.persistence.repository.ChannelMemberRepository;
import com.cloudmedia.content.persistence.repository.ChannelRepository;
import com.cloudmedia.content.persistence.repository.ContentRepository;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentService {

	private final ContentRepository contentRepository;
	private final ChannelRepository channelRepository;
	private final ChannelMemberRepository channelMemberRepository;
	private final ContentEventPublisher contentEventPublisher;

	public ContentService(ContentRepository contentRepository, ChannelRepository channelRepository,
			ChannelMemberRepository channelMemberRepository, ContentEventPublisher contentEventPublisher) {
		this.contentRepository = contentRepository;
		this.channelRepository = channelRepository;
		this.channelMemberRepository = channelMemberRepository;
		this.contentEventPublisher = contentEventPublisher;
	}

	@Transactional
	public ContentResponse createDraft(CreateContentRequest request) {
		return createDraft(request, null);
	}

	@Transactional
	public ContentResponse createDraft(CreateContentRequest request, String traceId) {
		ChannelEntity channel = channelRepository.findById(request.channelId()).orElseThrow(
				() -> new ApiException(HttpStatus.NOT_FOUND, "CHANNEL_NOT_FOUND", "Channel not found", null));
		assertMember(request.channelId(), request.userId());

		LocalDateTime now = LocalDateTime.now();
		ContentEntity content = new ContentEntity();
		content.setId(UUID.randomUUID().toString());
		content.setChannel(channel);
		content.setTitle(request.title());
		content.setDescription(request.description());
		content.setContentType(request.contentType());
		content.setState(ContentState.DRAFT);
		content.setVisibility(request.visibility() == null ? ContentVisibility.PRIVATE : request.visibility());
		content.setPlaybackReady(false);
		content.setCreatedAt(now);
		content.setUpdatedAt(now);
		content.setPublishedAt(null);

		ContentEntity savedContent = contentRepository.save(content);
		publishEvent("content.created", savedContent.getId(), traceId,
				new ContentCreatedPayload(savedContent.getId(), savedContent.getChannel().getId(),
						savedContent.getTitle(), savedContent.getContentType(), savedContent.getVisibility(),
						savedContent.getState()));
		return toResponse(savedContent);
	}

	@Transactional
	public ContentResponse updateMetadata(String contentId, UpdateContentRequest request) {
		return updateMetadata(contentId, request, null);
	}

	@Transactional
	public ContentResponse updateMetadata(String contentId, UpdateContentRequest request, String traceId) {
		ContentEntity content = contentRepository.findById(contentId).orElseThrow(
				() -> new ApiException(HttpStatus.NOT_FOUND, "CONTENT_NOT_FOUND", "Content not found", null));
		assertMember(content.getChannel().getId(), request.userId());

		if (request.title() != null) {
			content.setTitle(request.title());
		}
		if (request.description() != null) {
			content.setDescription(request.description());
		}
		if (request.visibility() != null) {
			content.setVisibility(request.visibility());
		}
		content.setUpdatedAt(LocalDateTime.now());

		ContentEntity savedContent = contentRepository.save(content);
		publishEvent("content.updated", savedContent.getId(), traceId,
				new ContentUpdatedPayload(savedContent.getId(), savedContent.getChannel().getId(),
						savedContent.getTitle(), savedContent.getContentType(), savedContent.getVisibility()));
		return toResponse(savedContent);
	}

	@Transactional(readOnly = true)
	public PlaybackResponse getPlayback(String contentId) {
		ContentEntity content = contentRepository.findById(contentId).orElseThrow(
				() -> new ApiException(HttpStatus.NOT_FOUND, "CONTENT_NOT_FOUND", "Content not found", null));

		if (content.getState() != ContentState.PUBLISHED) {
			throw new ApiException(HttpStatus.CONFLICT, "CONTENT_NOT_PLAYABLE",
					"Only published content is available for playback", null);
		}
		if (!content.isPlaybackReady()) {
			throw new ApiException(HttpStatus.CONFLICT, "CONTENT_NOT_PLAYBACK_READY", "Content playback is not ready",
					null);
		}

		return new PlaybackResponse(content.getId(), manifestUrlFor(content), true, defaultRenditions(content),
				content.getPublishedAt());
	}

	@Transactional
	public ContentResponse publish(String contentId, String userId) {
		return publish(contentId, userId, null);
	}

	@Transactional
	public ContentResponse publish(String contentId, String userId, String traceId) {
		ContentEntity content = contentRepository.findById(contentId).orElseThrow(
				() -> new ApiException(HttpStatus.NOT_FOUND, "CONTENT_NOT_FOUND", "Content not found", null));
		assertMember(content.getChannel().getId(), userId);

		if (content.getState() == ContentState.REMOVED) {
			throw new ApiException(HttpStatus.CONFLICT, "CONTENT_REMOVED", "Removed content cannot be published", null);
		}
		if (content.getState() == ContentState.PUBLISHED) {
			return toResponse(content);
		}
		if (!content.isPlaybackReady()) {
			throw new ApiException(HttpStatus.CONFLICT, "CONTENT_NOT_READY_FOR_PUBLISH",
					"Content is not playback ready", null);
		}

		LocalDateTime now = LocalDateTime.now();
		content.setState(ContentState.PUBLISHED);
		if (content.getPublishedAt() == null) {
			content.setPublishedAt(now);
		}
		content.setUpdatedAt(now);
		ContentEntity savedContent = contentRepository.save(content);
		publishEvent("content.published", savedContent.getId(), traceId,
				new ContentPublishedPayload(savedContent.getId(), savedContent.getChannel().getId(),
						savedContent.getTitle(), savedContent.getDescription(), savedContent.getContentType(),
						savedContent.getVisibility(), savedContent.getPublishedAt()));
		return toResponse(savedContent);
	}

	@Transactional
	public ContentResponse unpublish(String contentId, String userId) {
		return unpublish(contentId, userId, null);
	}

	@Transactional
	public ContentResponse unpublish(String contentId, String userId, String traceId) {
		ContentEntity content = contentRepository.findById(contentId).orElseThrow(
				() -> new ApiException(HttpStatus.NOT_FOUND, "CONTENT_NOT_FOUND", "Content not found", null));
		assertMember(content.getChannel().getId(), userId);

		if (content.getState() != ContentState.PUBLISHED) {
			throw new ApiException(HttpStatus.CONFLICT, "CONTENT_NOT_PUBLISHED",
					"Only published content can be unpublished", null);
		}

		ContentState previousState = content.getState();
		content.setState(ContentState.PRIVATE);
		content.setUpdatedAt(LocalDateTime.now());
		ContentEntity savedContent = contentRepository.save(content);
		publishEvent("content.unpublished", savedContent.getId(), traceId,
				new ContentUnpublishedPayload(savedContent.getId(), savedContent.getChannel().getId(), previousState,
						savedContent.getState(), savedContent.getPublishedAt()));
		return toResponse(savedContent);
	}

	private void assertMember(String channelId, String userId) {
		if (channelMemberRepository.findByChannel_IdAndUserId(channelId, userId).isEmpty()) {
			throw new ApiException(HttpStatus.FORBIDDEN, "CHANNEL_ACCESS_DENIED", "User is not a member of the channel",
					null);
		}
	}

	private ContentResponse toResponse(ContentEntity content) {
		return new ContentResponse(content.getId(), content.getChannel().getId(), content.getTitle(),
				content.getDescription(), content.getContentType(), content.getState(), content.getVisibility(),
				content.isPlaybackReady(), content.getCreatedAt(), content.getUpdatedAt(), content.getPublishedAt());
	}

	private String manifestUrlFor(ContentEntity content) {
		return "https://playback.cloudmedia.local/content/" + content.getId() + "/master.m3u8";
	}

	private List<PlaybackRenditionResponse> defaultRenditions(ContentEntity content) {
		return switch (content.getContentType()) {
			case VIDEO -> List.of(new PlaybackRenditionResponse("sd", "854x480", "h264-aac"),
					new PlaybackRenditionResponse("hd", "1920x1080", "h264-aac"));
			case MUSIC -> List.of(new PlaybackRenditionResponse("audio", "audio-only", "aac"));
		};
	}

	private void publishEvent(String eventType, String entityId, String traceId, Object payload) {
		contentEventPublisher.publish(new ContentEventEnvelope(UUID.randomUUID().toString(), eventType, 1,
				Instant.now(), "content-service", "content", entityId, traceId, payload));
	}
}
