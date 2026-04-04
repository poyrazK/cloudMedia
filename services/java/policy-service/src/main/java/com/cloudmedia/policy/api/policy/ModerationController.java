package com.cloudmedia.policy.api.policy;

import com.cloudmedia.policy.api.policy.dto.ContentPolicyResponse;
import com.cloudmedia.policy.api.policy.dto.UpdateModerationStateRequest;
import com.cloudmedia.policy.api.response.ApiMeta;
import com.cloudmedia.policy.api.response.ApiSuccessResponse;
import com.cloudmedia.policy.application.ContentPolicyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/v1/moderation")
public class ModerationController {

	private final ContentPolicyService contentPolicyService;

	public ModerationController(ContentPolicyService contentPolicyService) {
		this.contentPolicyService = contentPolicyService;
	}

	@PatchMapping("/content/{contentId}")
	public ResponseEntity<ApiSuccessResponse<ContentPolicyResponse>> updateModerationState(
			@PathVariable("contentId") @NotBlank String contentId,
			@Valid @RequestBody UpdateModerationStateRequest request,
			@RequestHeader(value = "X-Request-Id", required = false) String requestId) {
		String effectiveRequestId = requestId != null && !requestId.isBlank() ? requestId : "req_" + UUID.randomUUID();
		ContentPolicyResponse response = contentPolicyService.updateModerationState(contentId, request);
		return ResponseEntity.ok(new ApiSuccessResponse<>(response, new ApiMeta(effectiveRequestId, Instant.now())));
	}
}
