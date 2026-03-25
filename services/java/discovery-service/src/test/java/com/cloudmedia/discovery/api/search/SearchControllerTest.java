package com.cloudmedia.discovery.api.search;

import com.cloudmedia.discovery.discovery.HomeFeedCandidates;
import com.cloudmedia.discovery.policy.PolicyDecision;
import com.cloudmedia.discovery.policy.PolicyEvaluationClient;
import com.cloudmedia.discovery.search.SearchIndexReader;
import com.cloudmedia.discovery.search.SearchResponse;
import com.cloudmedia.discovery.search.SearchResultItem;
import com.cloudmedia.discovery.search.AutocompleteResponse;
import com.cloudmedia.discovery.search.AutocompleteSuggestion;
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
class SearchControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void searchReturnsResults() throws Exception {
		mockMvc.perform(get("/v1/search").param("q", "cats").param("page", "1").param("size", "10")
				.header("X-Request-Id", "req_search_1")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items[0].contentId").value("cnt_1"))
				.andExpect(jsonPath("$.data.page").value(1)).andExpect(jsonPath("$.data.size").value(10))
				.andExpect(jsonPath("$.data.total").value(1))
				.andExpect(jsonPath("$.meta.requestId").value("req_search_1"));
	}

	@Test
	void searchValidatesBlankQuery() throws Exception {
		mockMvc.perform(get("/v1/search").param("q", " ")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void searchFiltersPolicyBlockedItems() throws Exception {
		mockMvc.perform(get("/v1/search").param("q", "blocked").param("countryCode", "US").param("ageVerified", "true"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.items.length()").value(0))
				.andExpect(jsonPath("$.data.total").value(1));
	}

	@Test
	void searchRejectsInvalidCountryCodeFormat() throws Exception {
		mockMvc.perform(get("/v1/search").param("q", "cats").param("countryCode", "usa"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void autocompleteReturnsSuggestions() throws Exception {
		mockMvc.perform(get("/v1/search/autocomplete").param("q", "cat").param("size", "3").header("X-Request-Id",
				"req_autocomplete_1")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items[0].text").value("Cats video"))
				.andExpect(jsonPath("$.data.size").value(3))
				.andExpect(jsonPath("$.meta.requestId").value("req_autocomplete_1"));
	}

	@Test
	void autocompleteUsesDefaultSizeWhenOmitted() throws Exception {
		mockMvc.perform(get("/v1/search/autocomplete").param("q", "cat")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.size").value(5))
				.andExpect(jsonPath("$.data.items[0].text").value("Cats video"));
	}

	@Test
	void autocompleteValidatesSizeLimit() throws Exception {
		mockMvc.perform(get("/v1/search/autocomplete").param("q", "cat").param("size", "11"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void autocompleteRejectsZeroSize() throws Exception {
		mockMvc.perform(get("/v1/search/autocomplete").param("q", "cat").param("size", "0"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@TestConfiguration
	static class SearchReaderTestConfiguration {

		@Bean
		@Primary
		SearchIndexReader searchIndexReader() {
			return new SearchIndexReader() {
				@Override
				public SearchResponse search(String query, int page, int size) {
					List<SearchResultItem> items = "blocked".equals(query)
							? List.of(new SearchResultItem("cnt_blocked", "chn_1", "Blocked", "Blocked by policy",
									"VIDEO", "PUBLIC", Instant.parse("2026-03-14T12:00:00Z")))
							: List.of(new SearchResultItem("cnt_1", "chn_1", "Cats video", "Funny cats", "VIDEO",
									"PUBLIC", Instant.parse("2026-03-14T12:00:00Z")));
					return new SearchResponse(items, page, size, items.size());
				}

				@Override
				public AutocompleteResponse autocomplete(String query, int size) {
					return new AutocompleteResponse(List.of(new AutocompleteSuggestion("Cats video", "cnt_1", "chn_1")),
							size);
				}

				@Override
				public HomeFeedCandidates homeFeed(String userId, int size) {
					return HomeFeedCandidates.empty();
				}
			};
		}

		@Bean
		@Primary
		PolicyEvaluationClient policyEvaluationClient() {
			return (contentId, countryCode, ageVerified) -> {
				boolean allowed = !"cnt_blocked".equals(contentId);
				return new PolicyDecision(allowed, allowed ? List.of() : List.of("CONTENT_BLOCKED"));
			};
		}
	}
}
