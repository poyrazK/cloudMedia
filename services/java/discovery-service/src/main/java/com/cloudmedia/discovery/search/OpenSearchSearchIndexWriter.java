package com.cloudmedia.discovery.search;

import java.util.LinkedHashMap;
import java.util.Map;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class OpenSearchSearchIndexWriter implements SearchIndexWriter {

	private static final Logger LOGGER = LoggerFactory.getLogger(OpenSearchSearchIndexWriter.class);

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules()
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

	private final RestTemplate restTemplate;

	private final OpenSearchProperties properties;

	public OpenSearchSearchIndexWriter(RestTemplate restTemplate, OpenSearchProperties properties) {
		this.restTemplate = restTemplate;
		this.properties = properties;
	}

	@Override
	public void upsert(SearchDocument document) {
		try {
			restTemplate.exchange(documentUrl(document.contentId()), HttpMethod.PUT, jsonEntity(document), Void.class);
		} catch (RestClientException exception) {
			LOGGER.error("Failed OpenSearch upsert contentId={} indexAlias={} url={}", document.contentId(),
					properties.getIndexAlias(), documentUrl(document.contentId()), exception);
			throw new IllegalStateException("Failed to upsert search document " + document.contentId(), exception);
		}
	}

	@Override
	public void update(String contentId, SearchDocumentUpdate update) {
		Map<String, Object> doc = new LinkedHashMap<>();
		putIfPresent(doc, "channelId", update.channelId());
		putIfPresent(doc, "title", update.title());
		putIfPresent(doc, "description", update.description());
		putIfPresent(doc, "contentType", update.contentType());
		putIfPresent(doc, "visibility", update.visibility());
		if (doc.isEmpty()) {
			LOGGER.debug("Skipping empty partial update for contentId={}", contentId);
			return;
		}
		try {
			restTemplate.exchange(updateUrl(contentId), HttpMethod.POST, jsonEntity(Map.of("doc", doc)), Void.class);
		} catch (RestClientException exception) {
			LOGGER.error("Failed OpenSearch partial update contentId={} indexAlias={} url={}", contentId,
					properties.getIndexAlias(), updateUrl(contentId), exception);
			throw new IllegalStateException("Failed to update search document " + contentId, exception);
		}
	}

	@Override
	public void delete(String contentId) {
		try {
			restTemplate.exchange(documentUrl(contentId), HttpMethod.DELETE, HttpEntity.EMPTY, Void.class);
		} catch (HttpClientErrorException.NotFound ignored) {
			// delete remains idempotent when the document is already absent
		}
	}

	private HttpEntity<String> jsonEntity(Object body) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		try {
			return new HttpEntity<>(OBJECT_MAPPER.writeValueAsString(body), headers);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Failed to serialize search index payload", exception);
		}
	}

	private String documentUrl(String contentId) {
		return UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
				.pathSegment(properties.getIndexAlias(), "_doc", contentId).build().toUriString();
	}

	private String updateUrl(String contentId) {
		return UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
				.pathSegment(properties.getIndexAlias(), "_update", contentId).build().toUriString();
	}

	private void putIfPresent(Map<String, Object> doc, String key, Object value) {
		if (value != null) {
			doc.put(key, value);
		}
	}
}
