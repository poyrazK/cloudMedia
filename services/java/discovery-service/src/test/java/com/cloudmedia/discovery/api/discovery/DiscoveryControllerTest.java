package com.cloudmedia.discovery.api.discovery;

import com.cloudmedia.discovery.discovery.FeedSourceBucket;
import com.cloudmedia.discovery.discovery.HomeFeedCandidates;
import com.cloudmedia.discovery.discovery.HomeFeedItem;
import com.cloudmedia.discovery.discovery.HomeFeedResponse;
import com.cloudmedia.discovery.discovery.HomeFeedService;
import com.cloudmedia.discovery.search.AutocompleteResponse;
import com.cloudmedia.discovery.search.SearchIndexReader;
import com.cloudmedia.discovery.search.SearchResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DiscoveryControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void homeReturnsFeedItems() throws Exception {
		mockMvc.perform(get("/v1/discovery/home").param("size", "2").header("X-Request-Id", "req_home_1"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].contentId").value("cnt_1"))
				.andExpect(jsonPath("$.data.items[0].sourceBucket").value("TRENDING"))
				.andExpect(jsonPath("$.data.size").value(2))
				.andExpect(jsonPath("$.meta.requestId").value("req_home_1"));
	}

	@Test
	void homeValidatesSizeLimit() throws Exception {
		mockMvc.perform(get("/v1/discovery/home").param("size", "51")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@TestConfiguration
	static class HomeFeedTestConfiguration {

		@Bean
		@Primary
		HomeFeedService homeFeedService() {
			return new HomeFeedService(new SearchIndexReader() {
				@Override
				public SearchResponse search(String query, int page, int size) {
					return null;
				}

				@Override
				public AutocompleteResponse autocomplete(String query, int size) {
					return null;
				}

				@Override
				public HomeFeedCandidates homeFeed(String userId, int size) {
					return HomeFeedCandidates.empty();
				}
			}) {
				@Override
				public HomeFeedResponse homeFeed(String userId, Integer size) {
					return new HomeFeedResponse(List.of(new HomeFeedItem("cnt_1", "chn_1", "Title", "Description",
							"VIDEO", "PUBLIC", Instant.parse("2026-03-14T12:00:00Z"), FeedSourceBucket.TRENDING)), 2);
				}
			};
		}
	}
}
