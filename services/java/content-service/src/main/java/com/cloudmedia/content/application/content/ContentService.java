package com.cloudmedia.content.application.content;

import com.cloudmedia.content.api.content.dto.ContentResponse;
import com.cloudmedia.content.api.content.dto.CreateContentRequest;
import com.cloudmedia.content.api.content.dto.PlaybackResponse;
import com.cloudmedia.content.api.content.dto.UpdateContentRequest;
import com.cloudmedia.content.events.ContentEventPublisher;
import com.cloudmedia.content.events.ContentPublishedPayload;
import com.cloudmedia.content.events.ContentUpdatedPayload;
import com.cloudmedia.content.error.ApiException;
import com.cloudmedia.content.persistence.entity.ContentState;
import com.cloudmedia.content.persistence.entity.ChannelEntity;
import com.cloudmedia.content.persistence.entity.ContentEntity;
import com.cloudmedia.content.persistence.entity.ContentVisibility;
import com.cloudmedia.content.persistence.repository.ChannelMemberRepository;
import com.cloudmedia.content.persistence.repository.ChannelRepository;
import com.cloudmedia.content.persistence.repository.ContentRepository;
import com.cloudmedia.content.policy.PolicyDecision;
import com.cloudmedia.content.policy.PolicyEvaluationClient;
import com.cloudmedia.content.policy.PolicyEvaluationException;
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
	private final PolicyEvaluationClient policyEvaluationClient;
	private final ContentEventPublisher contentEventPublisher;

	public ContentService(ContentRepository contentRepository, ChannelRepository channelRepository,
			ChannelMemberRepository channelMemberRepository, PolicyEvaluationClient policyEvaluationClient,
			ContentEventPublisher contentEventPublisher) {
		this.contentRepository = contentRepository;
		this.channelRepository = channelRepository;
		this.channelMemberRepository = channelMemberRepository;
		this.policyEvaluationClient = policyEvaluationClient;
		this.contentEventPublisher = contentEventPublisher;
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

		ContentEntity savedContent = contentRepository.save(content);
		if (savedContent.getState() == ContentState.PUBLISHED) {
			contentEventPublisher.publishContentUpdated(new ContentUpdatedPayload(savedContent.getId(),
					savedContent.getChannel().getId(), savedContent.getTitle(), savedContent.getContentType().name(),
					savedContent.getVisibility().name()), null);
		}

		return toResponse(savedContent);
	}

	@Transactional
	public ContentResponse publish(String contentId, String userId) {
		ContentEntity content = contentRepository.findById(contentId).orElseThrow(
				() -> new ApiException(HttpStatus.NOT_FOUND, "CONTENT_NOT_FOUND", "Content not found", null));
		assertMember(content.getChannel().getId(), userId);

		if (content.getState() != ContentState.DRAFT) {
			throw new ApiException(HttpStatus.CONFLICT, "CONTENT_STATE_INVALID",
					"Content can only be published from draft state", null);
		}
		if (!content.isPlaybackReady()) {
			throw new ApiException(HttpStatus.CONFLICT, "CONTENT_NOT_READY", "Content is not ready for publish", null);
		}

		LocalDateTime now = LocalDateTime.now();
		content.setState(ContentState.PUBLISHED);
		content.setPublishedAt(now);
		content.setUpdatedAt(now);
		ContentEntity savedContent = contentRepository.save(content);
		contentEventPublisher.publishContentPublished(
				new ContentPublishedPayload(savedContent.getId(), savedContent.getChannel().getId(),
						savedContent.getTitle(), savedContent.getDescription(), savedContent.getContentType().name(),
						savedContent.getVisibility().name(), savedContent.getPublishedAt()),
				null);

		return toResponse(savedContent);
	}

	@Transactional
	public ContentResponse unpublish(String contentId, String userId) {
		ContentEntity content = contentRepository.findById(contentId).orElseThrow(
				() -> new ApiException(HttpStatus.NOT_FOUND, "CONTENT_NOT_FOUND", "Content not found", null));
		assertMember(content.getChannel().getId(), userId);

		if (content.getState() != ContentState.PUBLISHED) {
			throw new ApiException(HttpStatus.CONFLICT, "CONTENT_STATE_INVALID",
					"Content can only be unpublished from published state", null);
		}

		content.setState(ContentState.PRIVATE);
		content.setUpdatedAt(LocalDateTime.now());

		return toResponse(contentRepository.save(content));
	}

	@Transactional(readOnly = true)
	public PlaybackResponse getPlayback(String contentId, String countryCode, Boolean ageVerified) {
		ContentEntity content = contentRepository.findById(contentId).orElseThrow(
				() -> new ApiException(HttpStatus.NOT_FOUND, "CONTENT_NOT_FOUND", "Content not found", null));
		if (content.getState() != ContentState.PUBLISHED || !content.isPlaybackReady()) {
			throw new ApiException(HttpStatus.CONFLICT, "CONTENT_NOT_PLAYABLE", "Content is not ready for playback",
					null);
		}

		PolicyDecision decision;
		try {
			decision = policyEvaluationClient.evaluate(contentId, countryCode, ageVerified);
		} catch (PolicyEvaluationException exception) {
			throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "POLICY_SERVICE_UNAVAILABLE",
					"Policy evaluation is temporarily unavailable", null);
		}

		if (!decision.allowed()) {
			throw new ApiException(HttpStatus.FORBIDDEN, "CONTENT_POLICY_DENIED", "Content is blocked by policy", null);
		}

		return new PlaybackResponse(content.getId(), manifestUrlFor(content.getId()), List.of("1080p", "4K"));
	}

	private String manifestUrlFor(String contentId) {
		return "https://cdn.cloudmedia.local/v1/content/" + contentId + "/master.m3u8";
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
}
