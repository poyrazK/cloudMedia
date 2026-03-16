package com.cloudmedia.discovery.search;

import com.cloudmedia.discovery.discovery.HomeFeedCandidates;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenSearchSearchIndexReaderTest {

	private final OpenSearchProperties properties = new OpenSearchProperties();

	private RestTemplate restTemplate;

	private MockRestServiceServer server;

	private OpenSearchSearchIndexReader reader;

	@BeforeEach
	void setUp() {
		properties.setBaseUrl("http://localhost:9200");
		properties.setIndexAlias("content-read");
		restTemplate = new RestTemplate();
		server = MockRestServiceServer.createServer(restTemplate);
		reader = new OpenSearchSearchIndexReader(restTemplate, properties, new ObjectMapper().findAndRegisterModules());
	}

	@Test
	void searchQueriesConfiguredIndexAliasAndMapsHits() {
		server.expect(once(), requestTo("http://localhost:9200/content-read/_search"))
				.andExpect(method(HttpMethod.POST)).andExpect(content().json("""
						{
						  "from": 20,
						  "size": 20,
						  "query": {
						    "multi_match": {
						      "query": "cats",
						      "fields": ["title^2", "description"]
						    }
						  }
						}
						""", false)).andRespond(withSuccess("""
						{
						  "hits": {
						    "total": { "value": 1 },
						    "hits": [
						      {
						        "_source": {
						          "contentId": "cnt_1",
						          "channelId": "chn_1",
						          "title": "Cats video",
						          "description": "Funny cats",
						          "contentType": "VIDEO",
						          "visibility": "PUBLIC",
						          "publishedAt": "2026-03-14T12:00:00Z"
						        }
						      }
						    ]
						  }
						}
						""", org.springframework.http.MediaType.APPLICATION_JSON));

		SearchResponse response = reader.search("cats", 1, 20);

		assertEquals(1, response.total());
		assertEquals(1, response.items().size());
		assertEquals("cnt_1", response.items().getFirst().contentId());
		assertEquals(Instant.parse("2026-03-14T12:00:00Z"), response.items().getFirst().publishedAt());
	}

	@Test
	void searchWrapsRestFailures() {
		server.expect(once(), requestTo("http://localhost:9200/content-read/_search"))
				.andExpect(method(HttpMethod.POST)).andRespond(withStatus(INTERNAL_SERVER_ERROR));

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> reader.search("cats", 0, 20));

		assertEquals("Failed to search index for query cats", exception.getMessage());
	}

	@Test
	void autocompleteQueriesConfiguredIndexAliasAndMapsSuggestions() {
		server.expect(once(), requestTo("http://localhost:9200/content-read/_search"))
				.andExpect(method(HttpMethod.POST)).andExpect(content().json("""
						{
						  "size": 3,
						  "_source": ["contentId", "channelId", "title"],
						  "query": {
						    "match_phrase_prefix": {
						      "title": {
						        "query": "cat"
						      }
						    }
						  }
						}
						""", false)).andRespond(withSuccess("""
						{
						  "hits": {
						    "hits": [
						      {
						        "_source": {
						          "contentId": "cnt_1",
						          "channelId": "chn_1",
						          "title": "Cats video"
						        }
						      }
						    ]
						  }
						}
						""", org.springframework.http.MediaType.APPLICATION_JSON));

		AutocompleteResponse response = reader.autocomplete("cat", 3);

		assertEquals(1, response.items().size());
		assertEquals("Cats video", response.items().getFirst().text());
		assertEquals(3, response.size());
	}

	@Test
	void autocompleteWrapsRestFailures() {
		server.expect(once(), requestTo("http://localhost:9200/content-read/_search"))
				.andExpect(method(HttpMethod.POST)).andRespond(withStatus(INTERNAL_SERVER_ERROR));

		IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> reader.autocomplete("cat", 3));

		assertEquals("Failed to autocomplete query cat", exception.getMessage());
	}

	@Test
	void homeFeedReturnsTrendingAndFreshBuckets() {
		server.expect(once(), requestTo("http://localhost:9200/content-read/_search"))
				.andExpect(method(HttpMethod.POST)).andRespond(withSuccess("""
						{
						  "hits": {
						    "hits": [
						      {
						        "_source": {
						          "contentId": "trend_1",
						          "channelId": "chn_1",
						          "title": "Trending title",
						          "description": "Description",
						          "contentType": "VIDEO",
						          "visibility": "PUBLIC",
						          "publishedAt": "2026-03-14T12:00:00Z"
						        }
						      }
						    ]
						  }
						}
						""", org.springframework.http.MediaType.APPLICATION_JSON));
		server.expect(once(), requestTo("http://localhost:9200/content-read/_search"))
				.andExpect(method(HttpMethod.POST)).andRespond(withSuccess("""
						{
						  "hits": {
						    "hits": [
						      {
						        "_source": {
						          "contentId": "fresh_1",
						          "channelId": "chn_2",
						          "title": "Fresh title",
						          "description": "Description",
						          "contentType": "VIDEO",
						          "visibility": "PUBLIC",
						          "publishedAt": "2026-03-15T12:00:00Z"
						        }
						      }
						    ]
						  }
						}
						""", org.springframework.http.MediaType.APPLICATION_JSON));

		HomeFeedCandidates response = reader.homeFeed(null, 5);

		assertEquals(1, response.trending().size());
		assertEquals("trend_1", response.trending().getFirst().contentId());
		assertEquals(1, response.fresh().size());
		assertEquals("fresh_1", response.fresh().getFirst().contentId());
	}
}
