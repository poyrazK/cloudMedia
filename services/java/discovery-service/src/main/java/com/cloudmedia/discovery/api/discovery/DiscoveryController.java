package com.cloudmedia.discovery.api.discovery;

import com.cloudmedia.discovery.api.response.ApiMeta;
import com.cloudmedia.discovery.api.response.ApiSuccessResponse;
import com.cloudmedia.discovery.discovery.HomeFeedResponse;
import com.cloudmedia.discovery.discovery.HomeFeedService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/v1/discovery")
public class DiscoveryController {

	private final HomeFeedService homeFeedService;

	public DiscoveryController(HomeFeedService homeFeedService) {
		this.homeFeedService = homeFeedService;
	}

	@GetMapping("/home")
	public ResponseEntity<ApiSuccessResponse<HomeFeedResponse>> home(
			@RequestParam(value = "userId", required = false) String userId,
			@RequestParam(value = "size", required = false) @Min(1) @Max(50) Integer size,
			@RequestParam(value = "countryCode", required = false) @Pattern(regexp = "^[A-Z]{2}$", message = "must be an ISO 3166-1 alpha-2 uppercase code") String countryCode,
			@RequestParam(value = "ageVerified", required = false) Boolean ageVerified,
			@RequestHeader(value = "X-Request-Id", required = false) String requestId) {
		String effectiveRequestId = requestId(requestId);
		HomeFeedResponse response = homeFeedService.homeFeed(userId, size, countryCode, ageVerified);
		return ResponseEntity.ok(new ApiSuccessResponse<>(response, new ApiMeta(effectiveRequestId, Instant.now())));
	}

	private String requestId(String requestIdHeader) {
		return requestIdHeader != null && !requestIdHeader.isBlank() ? requestIdHeader : "req_" + UUID.randomUUID();
	}
}
