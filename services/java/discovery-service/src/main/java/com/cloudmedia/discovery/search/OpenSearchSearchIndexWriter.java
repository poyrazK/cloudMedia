package com.cloudmedia.discovery.search;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

public class OpenSearchSearchIndexWriter implements SearchIndexWriter {

	private final RestTemplate restTemplate;

	private final OpenSearchProperties properties;

	public OpenSearchSearchIndexWriter(RestTemplate restTemplate, OpenSearchProperties properties) {
		this.restTemplate = restTemplate;
		this.properties = properties;
	}

	@Override
	public void upsert(SearchDocument document) {
		restTemplate.exchange(documentUrl(document.contentId()), HttpMethod.PUT, jsonEntity(document), Void.class);
	}

	@Override
	public void delete(String contentId) {
		try {
			restTemplate.exchange(documentUrl(contentId), HttpMethod.DELETE, HttpEntity.EMPTY, Void.class);
		} catch (HttpClientErrorException.NotFound ignored) {
			// delete remains idempotent when the document is already absent
		}
	}

	private HttpEntity<SearchDocument> jsonEntity(SearchDocument document) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		return new HttpEntity<>(document, headers);
	}

	private String documentUrl(String contentId) {
		return properties.getBaseUrl() + "/" + properties.getIndexAlias() + "/_doc/" + contentId;
	}
}
