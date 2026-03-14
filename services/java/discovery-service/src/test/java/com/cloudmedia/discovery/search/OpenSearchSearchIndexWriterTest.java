package com.cloudmedia.discovery.search;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.springframework.test.web.client.ExpectedCount.once;
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
		server.expect(once(), requestTo("http://localhost:9200/content-read/_doc/cnt_1"))
				.andExpect(method(HttpMethod.PUT)).andRespond(withNoContent());

		writer.upsert(new SearchDocument("cnt_1", "chn_1", "Title", "Description", "VIDEO", "PUBLIC",
				LocalDateTime.parse("2026-03-14T12:00:00")));

		server.verify();
	}

	@Test
	void deleteIgnoresMissingDocumentResponses() {
		server.expect(once(), requestTo("http://localhost:9200/content-read/_doc/cnt_2"))
				.andExpect(method(HttpMethod.DELETE)).andRespond(withStatus(NOT_FOUND));

		writer.delete("cnt_2");

		server.verify();
	}
}
