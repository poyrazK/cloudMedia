package com.cloudmedia.policy.api.policy;

import com.cloudmedia.policy.api.policy.dto.ContentPolicyResponse;
import com.cloudmedia.policy.api.policy.dto.ContentPolicyDecisionResponse;
import com.cloudmedia.policy.api.policy.dto.EvaluateContentPolicyRequest;
import com.cloudmedia.policy.api.policy.dto.UpdateContentPolicyRequest;
import com.cloudmedia.policy.api.response.ApiMeta;
import com.cloudmedia.policy.api.response.ApiSuccessResponse;
import com.cloudmedia.policy.application.ContentPolicyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/v1/policy")
public class PolicyController {

	private final ContentPolicyService contentPolicyService;

	public PolicyController(ContentPolicyService contentPolicyService) {
		this.contentPolicyService = contentPolicyService;
	}

	@PatchMapping("/content/{contentId}")
	public ResponseEntity<ApiSuccessResponse<ContentPolicyResponse>> updateContentPolicy(
			@PathVariable("contentId") @NotBlank String contentId,
			@Valid @RequestBody UpdateContentPolicyRequest request,
			@RequestHeader(value = "X-Request-Id", required = false) String requestId) {
		String effectiveRequestId = resolveRequestId(requestId);
		ContentPolicyResponse response = contentPolicyService.updateContentPolicy(contentId, request);
		return ResponseEntity.ok(new ApiSuccessResponse<>(response, new ApiMeta(effectiveRequestId, Instant.now())));
	}

	@PostMapping("/content/{contentId}/evaluate")
	public ResponseEntity<ApiSuccessResponse<ContentPolicyDecisionResponse>> evaluateContentPolicy(
			@PathVariable("contentId") @NotBlank String contentId,
			@Valid @RequestBody EvaluateContentPolicyRequest request,
			@RequestHeader(value = "X-Request-Id", required = false) String requestId) {
		String effectiveRequestId = resolveRequestId(requestId);
		ContentPolicyDecisionResponse response = contentPolicyService.evaluateContentPolicy(contentId, request);
		return ResponseEntity.ok(new ApiSuccessResponse<>(response, new ApiMeta(effectiveRequestId, Instant.now())));
	}

	private String resolveRequestId(String requestId) {
		return requestId != null && !requestId.isBlank() ? requestId : "req_" + UUID.randomUUID();
	}
}
