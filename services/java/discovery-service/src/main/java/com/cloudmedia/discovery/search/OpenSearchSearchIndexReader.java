package com.cloudmedia.discovery.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class OpenSearchSearchIndexReader implements SearchIndexReader {

	private static final Logger LOGGER = LoggerFactory.getLogger(OpenSearchSearchIndexReader.class);

	private final RestTemplate restTemplate;

	private final OpenSearchProperties properties;

	private final ObjectMapper objectMapper;

	public OpenSearchSearchIndexReader(RestTemplate restTemplate, OpenSearchProperties properties,
			ObjectMapper objectMapper) {
		this.restTemplate = restTemplate;
		this.properties = properties;
		this.objectMapper = objectMapper;
	}

	@Override
	public SearchResponse search(String query, int page, int size) {
		try {
			ResponseEntity<JsonNode> response = restTemplate.exchange(searchUrl(), HttpMethod.POST,
					jsonEntity(searchBody(query, page, size)), JsonNode.class);
			JsonNode body = response.getBody();
			if (body == null) {
				throw new IllegalStateException("OpenSearch returned empty body for searchUrl=" + searchUrl()
						+ " query=" + query + " page=" + page + " size=" + size);
			}
			return mapResponse(body, page, size);
		} catch (RestClientException exception) {
			LOGGER.error("Failed OpenSearch search query={} page={} size={} indexAlias={}", query, page, size,
					properties.getIndexAlias(), exception);
			throw new IllegalStateException("Failed to search index for query " + query, exception);
		}
	}

	@Override
	public AutocompleteResponse autocomplete(String query, int size) {
		try {
			ResponseEntity<JsonNode> response = restTemplate.exchange(searchUrl(), HttpMethod.POST,
					jsonEntity(autocompleteBody(query, size)), JsonNode.class);
			JsonNode body = response.getBody();
			if (body == null) {
				throw new IllegalStateException("OpenSearch returned empty body for autocomplete searchUrl="
						+ searchUrl() + " query=" + query + " size=" + size);
			}
			return mapAutocompleteResponse(body, size);
		} catch (RestClientException exception) {
			LOGGER.error("Failed OpenSearch autocomplete query={} size={} indexAlias={}", query, size,
					properties.getIndexAlias(), exception);
			throw new IllegalStateException("Failed to autocomplete query " + query, exception);
		}
	}

	private SearchResponse mapResponse(JsonNode body, int page, int size) {
		JsonNode hitsNode = body.path("hits");
		long total = hitsNode.path("total").path("value").asLong(0);
		List<SearchResultItem> items = new ArrayList<>();
		for (JsonNode hit : hitsNode.path("hits")) {
			JsonNode source = hit.path("_source");
			items.add(new SearchResultItem(source.path("contentId").asText(), source.path("channelId").asText(),
					source.path("title").asText(), textOrNull(source, "description"),
					source.path("contentType").asText(), source.path("visibility").asText(),
					instantOrNull(source, "publishedAt")));
		}
		return new SearchResponse(items, page, size, total);
	}

	private AutocompleteResponse mapAutocompleteResponse(JsonNode body, int size) {
		List<AutocompleteSuggestion> items = new ArrayList<>();
		for (JsonNode hit : body.path("hits").path("hits")) {
			JsonNode source = hit.path("_source");
			items.add(new AutocompleteSuggestion(textOrNull(source, "title"), textOrNull(source, "contentId"),
					textOrNull(source, "channelId")));
		}
		return new AutocompleteResponse(items, size);
	}

	private HttpEntity<Map<String, Object>> jsonEntity(Map<String, Object> body) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		return new HttpEntity<>(body, headers);
	}

	private Map<String, Object> searchBody(String query, int page, int size) {
		long rawFrom = (long) page * (long) size;
		int safeFrom = rawFrom < 0 ? 0 : (int) Math.min(rawFrom, Integer.MAX_VALUE);
		return Map.of("from", safeFrom, "size", size, "query",
				Map.of("multi_match", Map.of("query", query, "fields", List.of("title^2", "description"))), "sort",
				List.of(Map.of("_score", "desc"), Map.of("publishedAt", Map.of("order", "desc", "missing", "_last"))));
	}

	private Map<String, Object> autocompleteBody(String query, int size) {
		return Map.of("size", size, "_source", List.of("contentId", "channelId", "title"), "query",
				Map.of("match_phrase_prefix", Map.of("title", Map.of("query", query))), "sort",
				List.of(Map.of("publishedAt", Map.of("order", "desc", "missing", "_last"))));
	}

	private String searchUrl() {
		return UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
				.pathSegment(properties.getIndexAlias(), "_search").build().toUriString();
	}

	private String textOrNull(JsonNode source, String field) {
		return source.hasNonNull(field) ? source.path(field).asText() : null;
	}

	private Instant instantOrNull(JsonNode source, String field) {
		if (!source.hasNonNull(field)) {
			return null;
		}
		String value = source.path(field).asText(null);
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return Instant.parse(value);
		} catch (DateTimeException exception) {
			LOGGER.warn("Ignoring malformed instant field={} value={}", field, value);
			return null;
		}
	}
}
