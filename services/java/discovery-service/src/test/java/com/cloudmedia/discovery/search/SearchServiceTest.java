package com.cloudmedia.discovery.search;

import com.cloudmedia.discovery.discovery.HomeFeedCandidates;
import com.cloudmedia.discovery.policy.PolicyDecision;
import com.cloudmedia.discovery.policy.PolicyEvaluationClient;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchServiceTest {

	@Test
	void searchUsesDefaultPagingWhenParametersMissing() {
		RecordingSearchIndexReader reader = new RecordingSearchIndexReader();
		RecordingPolicyEvaluationClient policyClient = new RecordingPolicyEvaluationClient();
		SearchService service = new SearchService(reader, policyClient);

		SearchResponse response = service.search("video", null, null, "US", true);

		assertEquals("video", reader.query);
		assertEquals(0, reader.page);
		assertEquals(20, reader.size);
		assertEquals("cnt_1", policyClient.recordedContentIds.get(0));
		assertEquals("US", policyClient.recordedCountryCodes.get(0));
		assertEquals(Boolean.TRUE, policyClient.recordedAgeVerified.get(0));
		assertEquals(1, response.items().size());
	}

	@Test
	void searchClampsPagingValues() {
		RecordingSearchIndexReader reader = new RecordingSearchIndexReader();
		SearchService service = new SearchService(reader, new RecordingPolicyEvaluationClient());

		service.search("video", -5, 1000, null, null);

		assertEquals(0, reader.page);
		assertEquals(100, reader.size);
	}

	@Test
	void searchClampsNonPositiveSize() {
		RecordingSearchIndexReader reader = new RecordingSearchIndexReader();
		SearchService service = new SearchService(reader, new RecordingPolicyEvaluationClient());

		service.search("video", 1, 0, null, null);

		assertEquals(SearchService.MIN_SIZE, reader.size);
	}

	@Test
	void searchFiltersPolicyDeniedItems() {
		RecordingSearchIndexReader reader = new RecordingSearchIndexReader();
		reader.resultItems = List.of(
				new SearchResultItem("cnt_allowed", "chn_1", "Allowed", "Desc", "VIDEO", "PUBLIC",
						Instant.parse("2026-03-14T12:00:00Z")),
				new SearchResultItem("cnt_blocked", "chn_2", "Blocked", "Desc", "VIDEO", "PUBLIC",
						Instant.parse("2026-03-14T12:00:00Z")));
		RecordingPolicyEvaluationClient policyClient = new RecordingPolicyEvaluationClient();
		policyClient.blockedContentIds = List.of("cnt_blocked");
		SearchService service = new SearchService(reader, policyClient);

		SearchResponse response = service.search("video", 0, 10, "US", true);

		assertEquals(1, response.items().size());
		assertEquals("cnt_allowed", response.items().get(0).contentId());
		assertEquals(2, response.total());
		assertEquals(List.of("cnt_allowed", "cnt_blocked"), policyClient.recordedContentIds);
	}

	@Test
	void autocompleteUsesDefaultSizeWhenMissing() {
		RecordingSearchIndexReader reader = new RecordingSearchIndexReader();
		SearchService service = new SearchService(reader, new RecordingPolicyEvaluationClient());

		AutocompleteResponse response = service.autocomplete("cat", null);

		assertEquals("cat", reader.autocompleteQuery);
		assertEquals(5, reader.autocompleteSize);
		assertEquals(1, response.items().size());
	}

	@Test
	void autocompleteClampsSizeToMaximum() {
		RecordingSearchIndexReader reader = new RecordingSearchIndexReader();
		SearchService service = new SearchService(reader, new RecordingPolicyEvaluationClient());

		service.autocomplete("cat", 100);

		assertEquals(10, reader.autocompleteSize);
	}

	@Test
	void autocompleteClampsSizeToMinimum() {
		RecordingSearchIndexReader reader = new RecordingSearchIndexReader();
		SearchService service = new SearchService(reader, new RecordingPolicyEvaluationClient());

		service.autocomplete("cat", 0);

		assertEquals(SearchService.MIN_SIZE, reader.autocompleteSize);
	}

	static class RecordingSearchIndexReader implements SearchIndexReader {

		private List<SearchResultItem> resultItems = List.of(new SearchResultItem("cnt_1", "chn_1", "Title",
				"Description", "VIDEO", "PUBLIC", Instant.parse("2026-03-14T12:00:00Z")));

		private String query;

		private int page;

		private int size;

		private String autocompleteQuery;

		private int autocompleteSize;

		@Override
		public SearchResponse search(String query, int page, int size) {
			this.query = query;
			this.page = page;
			this.size = size;
			return new SearchResponse(resultItems, page, size, resultItems.size());
		}

		@Override
		public AutocompleteResponse autocomplete(String query, int size) {
			this.autocompleteQuery = query;
			this.autocompleteSize = size;
			return new AutocompleteResponse(List.of(new AutocompleteSuggestion("Title", "cnt_1", "chn_1")), size);
		}

		@Override
		public HomeFeedCandidates homeFeed(String userId, int size) {
			return HomeFeedCandidates.empty();
		}
	}

	static class RecordingPolicyEvaluationClient implements PolicyEvaluationClient {

		private final List<String> recordedContentIds = new java.util.ArrayList<>();

		private final List<String> recordedCountryCodes = new java.util.ArrayList<>();

		private final List<Boolean> recordedAgeVerified = new java.util.ArrayList<>();

		private List<String> blockedContentIds = List.of();

		@Override
		public PolicyDecision evaluate(String contentId, String countryCode, Boolean ageVerified) {
			recordedContentIds.add(contentId);
			recordedCountryCodes.add(countryCode);
			recordedAgeVerified.add(ageVerified);
			boolean allowed = !blockedContentIds.contains(contentId);
			return new PolicyDecision(allowed, allowed ? List.of() : List.of("CONTENT_BLOCKED"));
		}
	}
}
