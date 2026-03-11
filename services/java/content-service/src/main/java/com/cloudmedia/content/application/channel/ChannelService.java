package com.cloudmedia.content.application.channel;

import com.cloudmedia.content.api.channel.dto.ChannelResponse;
import com.cloudmedia.content.api.channel.dto.CreateChannelRequest;
import com.cloudmedia.content.api.channel.dto.UserChannelResponse;
import com.cloudmedia.content.error.ApiException;
import com.cloudmedia.content.persistence.entity.ChannelEntity;
import com.cloudmedia.content.persistence.entity.ChannelMemberEntity;
import com.cloudmedia.content.persistence.entity.ChannelMemberRole;
import com.cloudmedia.content.persistence.repository.ChannelMemberRepository;
import com.cloudmedia.content.persistence.repository.ChannelRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChannelService {

	private final ChannelRepository channelRepository;
	private final ChannelMemberRepository channelMemberRepository;

	public ChannelService(ChannelRepository channelRepository, ChannelMemberRepository channelMemberRepository) {
		this.channelRepository = channelRepository;
		this.channelMemberRepository = channelMemberRepository;
	}

	@Transactional
	public UserChannelResponse createChannel(CreateChannelRequest request) {
		if (channelRepository.findBySlug(request.slug()).isPresent()) {
			throw new ApiException(HttpStatus.CONFLICT, "CHANNEL_SLUG_EXISTS", "Channel slug already exists", null);
		}

		LocalDateTime now = LocalDateTime.now();
		ChannelEntity channel = new ChannelEntity();
		channel.setId(UUID.randomUUID().toString());
		channel.setSlug(request.slug());
		channel.setDisplayName(request.displayName());
		channel.setDescription(request.description());
		channel.setCreatedAt(now);
		channel.setUpdatedAt(now);

		try {
			ChannelEntity savedChannel = channelRepository.save(channel);

			ChannelMemberEntity member = new ChannelMemberEntity();
			member.setId(UUID.randomUUID().toString());
			member.setChannel(savedChannel);
			member.setUserId(request.ownerUserId());
			member.setRole(ChannelMemberRole.OWNER);
			member.setJoinedAt(now);
			channelMemberRepository.save(member);

			return new UserChannelResponse(toResponse(savedChannel), ChannelMemberRole.OWNER);
		} catch (DataIntegrityViolationException exception) {
			throw new ApiException(HttpStatus.CONFLICT, "CHANNEL_CONFLICT", "Channel creation conflict", null);
		}
	}

	@Transactional(readOnly = true)
	public ChannelResponse getById(String channelId) {
		ChannelEntity channel = channelRepository.findById(channelId).orElseThrow(
				() -> new ApiException(HttpStatus.NOT_FOUND, "CHANNEL_NOT_FOUND", "Channel not found", null));
		return toResponse(channel);
	}

	@Transactional(readOnly = true)
	public ChannelResponse getBySlug(String slug) {
		ChannelEntity channel = channelRepository.findBySlug(slug).orElseThrow(
				() -> new ApiException(HttpStatus.NOT_FOUND, "CHANNEL_NOT_FOUND", "Channel not found", null));
		return toResponse(channel);
	}

	@Transactional(readOnly = true)
	public List<UserChannelResponse> listByUserId(String userId) {
		return channelMemberRepository.findByUserId(userId).stream()
				.map(member -> new UserChannelResponse(toResponse(member.getChannel()), member.getRole())).toList();
	}

	private ChannelResponse toResponse(ChannelEntity channel) {
		return new ChannelResponse(channel.getId(), channel.getSlug(), channel.getDisplayName(),
				channel.getDescription(), channel.getCreatedAt(), channel.getUpdatedAt());
	}
}
