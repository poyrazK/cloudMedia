package com.cloudmedia.discovery.api.search;

import com.cloudmedia.discovery.api.response.ApiMeta;
import com.cloudmedia.discovery.api.response.ApiSuccessResponse;
import com.cloudmedia.discovery.search.AutocompleteResponse;
import com.cloudmedia.discovery.search.SearchResponse;
import com.cloudmedia.discovery.search.SearchService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
@RequestMapping("/v1")
public class SearchController {

	private final SearchService searchService;

	public SearchController(SearchService searchService) {
		this.searchService = searchService;
	}

	@GetMapping("/search")
	public ResponseEntity<ApiSuccessResponse<SearchResponse>> search(@RequestParam("q") @NotBlank String query,
			@RequestParam(value = "page", required = false) @Min(0) Integer page,
			@RequestParam(value = "size", required = false) @Min(1) @Max(100) Integer size,
			@RequestHeader(value = "X-Request-Id", required = false) String requestId) {
		String effectiveRequestId = requestId(requestId);
		SearchResponse response = searchService.search(query, page, size);
		return ResponseEntity.ok(new ApiSuccessResponse<>(response, new ApiMeta(effectiveRequestId, Instant.now())));
	}

	@GetMapping("/search/autocomplete")
	public ResponseEntity<ApiSuccessResponse<AutocompleteResponse>> autocomplete(
			@RequestParam("q") @NotBlank String query,
			@RequestParam(value = "size", required = false) @Min(1) @Max(10) Integer size,
			@RequestHeader(value = "X-Request-Id", required = false) String requestId) {
		String effectiveRequestId = requestId(requestId);
		AutocompleteResponse response = searchService.autocomplete(query, size);
		return ResponseEntity.ok(new ApiSuccessResponse<>(response, new ApiMeta(effectiveRequestId, Instant.now())));
	}

	private String requestId(String requestIdHeader) {
		return requestIdHeader != null && !requestIdHeader.isBlank() ? requestIdHeader : "req_" + UUID.randomUUID();
	}
}
