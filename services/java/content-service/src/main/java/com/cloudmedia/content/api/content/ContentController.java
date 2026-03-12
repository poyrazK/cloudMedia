package com.cloudmedia.content.api.content;

import com.cloudmedia.content.api.content.dto.ContentResponse;
import com.cloudmedia.content.api.content.dto.ContentActionRequest;
import com.cloudmedia.content.api.content.dto.CreateContentRequest;
import com.cloudmedia.content.api.content.dto.UpdateContentRequest;
import com.cloudmedia.content.api.response.ApiMeta;
import com.cloudmedia.content.api.response.ApiSuccessResponse;
import com.cloudmedia.content.application.content.ContentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/v1")
public class ContentController {

	private final ContentService contentService;

	public ContentController(ContentService contentService) {
		this.contentService = contentService;
	}

	@PostMapping("/content")
	public ResponseEntity<ApiSuccessResponse<ContentResponse>> createContentDraft(
			@Valid @RequestBody CreateContentRequest request,
			@RequestHeader(value = "X-Request-Id", required = false) String requestId) {
		ContentResponse response = contentService.createDraft(request);
		return ResponseEntity.ok(new ApiSuccessResponse<>(response, meta(requestId)));
	}

	@PatchMapping("/content/{contentId}")
	public ResponseEntity<ApiSuccessResponse<ContentResponse>> updateContentMetadata(
			@PathVariable("contentId") @NotBlank String contentId, @Valid @RequestBody UpdateContentRequest request,
			@RequestHeader(value = "X-Request-Id", required = false) String requestId) {
		ContentResponse response = contentService.updateMetadata(contentId, request);
		return ResponseEntity.ok(new ApiSuccessResponse<>(response, meta(requestId)));
	}

	@PostMapping("/content/{contentId}/publish")
	public ResponseEntity<ApiSuccessResponse<ContentResponse>> publishContent(
			@PathVariable("contentId") @NotBlank String contentId, @Valid @RequestBody ContentActionRequest request,
			@RequestHeader(value = "X-Request-Id", required = false) String requestId) {
		ContentResponse response = contentService.publish(contentId, request.userId());
		return ResponseEntity.ok(new ApiSuccessResponse<>(response, meta(requestId)));
	}

	@PostMapping("/content/{contentId}/unpublish")
	public ResponseEntity<ApiSuccessResponse<ContentResponse>> unpublishContent(
			@PathVariable("contentId") @NotBlank String contentId, @Valid @RequestBody ContentActionRequest request,
			@RequestHeader(value = "X-Request-Id", required = false) String requestId) {
		ContentResponse response = contentService.unpublish(contentId, request.userId());
		return ResponseEntity.ok(new ApiSuccessResponse<>(response, meta(requestId)));
	}

	private ApiMeta meta(String requestIdHeader) {
		String requestId = requestIdHeader != null && !requestIdHeader.isBlank()
				? requestIdHeader
				: "req_" + UUID.randomUUID();
		return new ApiMeta(requestId, Instant.now());
	}
}
