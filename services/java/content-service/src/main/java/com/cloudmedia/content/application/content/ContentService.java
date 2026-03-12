package com.cloudmedia.content.application.content;

import com.cloudmedia.content.api.content.dto.ContentResponse;
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
