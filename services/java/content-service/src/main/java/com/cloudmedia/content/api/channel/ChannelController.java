package com.cloudmedia.content.api.channel;

import com.cloudmedia.content.api.channel.dto.ChannelResponse;
import com.cloudmedia.content.api.channel.dto.CreateChannelRequest;
import com.cloudmedia.content.api.channel.dto.UserChannelResponse;
import com.cloudmedia.content.api.response.ApiMeta;
import com.cloudmedia.content.api.response.ApiSuccessResponse;
import com.cloudmedia.content.application.channel.ChannelService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/v1")
public class ChannelController {

	private final ChannelService channelService;

	public ChannelController(ChannelService channelService) {
		this.channelService = channelService;
	}

	@PostMapping("/channels")
	public ResponseEntity<ApiSuccessResponse<UserChannelResponse>> createChannel(
			@Valid @RequestBody CreateChannelRequest request,
			@RequestHeader(value = "X-Request-Id", required = false) String requestId) {
		UserChannelResponse response = channelService.createChannel(request);
		return ResponseEntity.ok(new ApiSuccessResponse<>(response, meta(requestId)));
	}

	@GetMapping("/channels/{channelId}")
	public ResponseEntity<ApiSuccessResponse<ChannelResponse>> getChannelById(
			@PathVariable("channelId") @NotBlank String channelId,
			@RequestHeader(value = "X-Request-Id", required = false) String requestId) {
		ChannelResponse response = channelService.getById(channelId);
		return ResponseEntity.ok(new ApiSuccessResponse<>(response, meta(requestId)));
	}

	@GetMapping("/channels/slug/{slug}")
	public ResponseEntity<ApiSuccessResponse<ChannelResponse>> getChannelBySlug(
			@PathVariable("slug") @NotBlank String slug,
			@RequestHeader(value = "X-Request-Id", required = false) String requestId) {
		ChannelResponse response = channelService.getBySlug(slug);
		return ResponseEntity.ok(new ApiSuccessResponse<>(response, meta(requestId)));
	}

	@GetMapping("/users/{userId}/channels")
	public ResponseEntity<ApiSuccessResponse<List<UserChannelResponse>>> listChannelsByUser(
			@PathVariable("userId") @NotBlank String userId,
			@RequestHeader(value = "X-Request-Id", required = false) String requestId) {
		List<UserChannelResponse> response = channelService.listByUserId(userId);
		return ResponseEntity.ok(new ApiSuccessResponse<>(response, meta(requestId)));
	}

	private ApiMeta meta(String requestIdHeader) {
		String requestId = requestIdHeader != null && !requestIdHeader.isBlank()
				? requestIdHeader
				: "req_" + UUID.randomUUID();
		return new ApiMeta(requestId, Instant.now());
	}
}
