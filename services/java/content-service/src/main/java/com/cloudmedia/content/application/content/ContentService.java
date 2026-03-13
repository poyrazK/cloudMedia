package com.cloudmedia.content.application.content;

import com.cloudmedia.content.api.content.dto.ContentResponse;
import com.cloudmedia.content.api.content.dto.PlaybackResponse;
import com.cloudmedia.content.api.content.dto.PlaybackRenditionResponse;
import com.cloudmedia.content.api.content.dto.CreateContentRequest;
import com.cloudmedia.content.api.content.dto.UpdateContentRequest;
import com.cloudmedia.content.error.ApiException;
import com.cloudmedia.content.persistence.entity.ChannelEntity;
import com.cloudmedia.content.persistence.entity.ContentEntity;
import com.cloudmedia.content.persistence.entity.ContentState;
import com.cloudmedia.content.persistence.entity.ContentVisibility;
import com.cloudmedia.content.persistence.repository.ChannelMemberRepository;
import com.cloudmedia.content.persistence.repository.ChannelRepository;
import com.cloudmedia.content.persistence.repository.ContentRepository;
import java.time.LocalDateTime;
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

	public ContentService(ContentRepository contentRepository, ChannelRepository channelRepository,
			ChannelMemberRepository channelMemberRepository) {
		this.contentRepository = contentRepository;
		this.channelRepository = channelRepository;
		this.channelMemberRepository = channelMemberRepository;
	}

	@Transactional
	public ContentResponse createDraft(CreateContentRequest request) {
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

		return toResponse(contentRepository.save(content));
	}

	@Transactional
	public ContentResponse updateMetadata(String contentId, UpdateContentRequest request) {
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

		return toResponse(contentRepository.save(content));
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
		return toResponse(contentRepository.save(content));
	}

	@Transactional
	public ContentResponse unpublish(String contentId, String userId) {
		ContentEntity content = contentRepository.findById(contentId).orElseThrow(
				() -> new ApiException(HttpStatus.NOT_FOUND, "CONTENT_NOT_FOUND", "Content not found", null));
		assertMember(content.getChannel().getId(), userId);

		if (content.getState() != ContentState.PUBLISHED) {
			throw new ApiException(HttpStatus.CONFLICT, "CONTENT_NOT_PUBLISHED",
					"Only published content can be unpublished", null);
		}

		content.setState(ContentState.PRIVATE);
		content.setUpdatedAt(LocalDateTime.now());
		return toResponse(contentRepository.save(content));
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
}
