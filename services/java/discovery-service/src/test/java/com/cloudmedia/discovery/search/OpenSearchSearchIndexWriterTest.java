package com.cloudmedia.discovery.search;

import java.time.Instant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.http.HttpStatus.NOT_FOUND;

class OpenSearchSearchIndexWriterTest {

	private final OpenSearchProperties properties = new OpenSearchProperties();

	private RestTemplate restTemplate;

	private MockRestServiceServer server;

	private OpenSearchSearchIndexWriter writer;

	@BeforeEach
	void setUp() {
		properties.setBaseUrl("http://localhost:9200");
		properties.setIndexAlias("content-read");
		restTemplate = new RestTemplate();
		server = MockRestServiceServer.createServer(restTemplate);
		writer = new OpenSearchSearchIndexWriter(restTemplate, properties);
	}

	@Test
	void upsertWritesDocumentToConfiguredIndexAlias() {
		SearchDocument document = new SearchDocument("cnt_1", "chn_1", "Title", "Description", "VIDEO", "PUBLIC",
				Instant.parse("2026-03-14T12:00:00Z"));

		server.expect(once(), requestTo("http://localhost:9200/content-read/_doc/cnt_1"))
				.andExpect(method(HttpMethod.PUT)).andExpect(content().json("""
						{
						  "contentId": "cnt_1",
						  "channelId": "chn_1",
						  "title": "Title",
						  "description": "Description",
						  "contentType": "VIDEO",
						  "visibility": "PUBLIC",
						  "publishedAt": "2026-03-14T12:00:00Z"
						}
						""")).andRespond(withNoContent());

		writer.upsert(document);

		server.verify();
	}

	@Test
	void updateWritesPartialDocumentToUpdateEndpoint() {
		server.expect(once(), requestTo("http://localhost:9200/content-read/_update/cnt_1"))
				.andExpect(method(HttpMethod.POST)).andExpect(content().json("""
						{
						  "doc": {
						    "channelId": "chn_1",
						    "title": "Updated Title",
						    "contentType": "VIDEO",
						    "visibility": "UNLISTED"
						  }
						}
						""")).andRespond(withNoContent());

		writer.update("cnt_1", new SearchDocumentUpdate("chn_1", "Updated Title", null, "VIDEO", "UNLISTED"));

		server.verify();
	}

	@Test
	void deleteIgnoresMissingDocumentResponses() {
		server.expect(once(), requestTo("http://localhost:9200/content-read/_doc/cnt_2"))
				.andExpect(method(HttpMethod.DELETE)).andRespond(withStatus(NOT_FOUND));

		writer.delete("cnt_2");

		server.verify();
	}

	@Test
	void upsertWrapsRestFailures() {
		server.expect(once(), requestTo("http://localhost:9200/content-read/_doc/cnt_3"))
				.andExpect(method(HttpMethod.PUT))
				.andRespond(withStatus(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR));

		IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class,
				() -> writer.upsert(new SearchDocument("cnt_3", "chn_3", "Title", "Description", "VIDEO", "PUBLIC",
						Instant.parse("2026-03-14T12:00:00Z"))));

		Assertions.assertEquals("Failed to upsert search document cnt_3", exception.getMessage());
	}
}
