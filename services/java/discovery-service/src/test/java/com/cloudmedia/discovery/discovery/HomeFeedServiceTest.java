package com.cloudmedia.discovery.discovery;

import com.cloudmedia.discovery.search.AutocompleteResponse;
import com.cloudmedia.discovery.policy.PolicyDecision;
import com.cloudmedia.discovery.policy.PolicyEvaluationClient;
import com.cloudmedia.discovery.search.SearchIndexReader;
import com.cloudmedia.discovery.search.SearchResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HomeFeedServiceTest {

	@Test
	void homeFeedUsesDefaultSizeAndBlendsDedupedItems() {
		RecordingSearchIndexReader reader = new RecordingSearchIndexReader();
		RecordingPolicyEvaluationClient policyClient = new RecordingPolicyEvaluationClient();
		HomeFeedService service = new HomeFeedService(reader, policyClient);

		HomeFeedResponse response = service.homeFeed(null, null, "US", true);

		assertEquals(20, reader.size);
		assertEquals(List.of("follow-1", "trend-1", "similar-1"), policyClient.recordedContentIds);
		assertEquals(3, response.items().size());
		assertEquals(FeedSourceBucket.FOLLOWED, response.items().get(0).sourceBucket());
		assertEquals(FeedSourceBucket.TRENDING, response.items().get(1).sourceBucket());
	}

	@Test
	void homeFeedClampsSizeAndAvoidsDuplicateContentIds() {
		RecordingSearchIndexReader reader = new RecordingSearchIndexReader();
		HomeFeedService service = new HomeFeedService(reader, new RecordingPolicyEvaluationClient());

		HomeFeedResponse response = service.homeFeed("user-1", 2, null, null);

		assertEquals("user-1", reader.userId);
		assertEquals(2, response.size());
		assertEquals(2, response.items().size());
		assertEquals(List.of("follow-1", "trend-1"), response.items().stream().map(HomeFeedItem::contentId).toList());
	}

	@Test
	void homeFeedFiltersPolicyBlockedItems() {
		RecordingSearchIndexReader reader = new RecordingSearchIndexReader();
		RecordingPolicyEvaluationClient policyClient = new RecordingPolicyEvaluationClient();
		policyClient.blockedContentIds = List.of("trend-1");
		HomeFeedService service = new HomeFeedService(reader, policyClient);

		HomeFeedResponse response = service.homeFeed("user-1", 3, "DE", true);

		assertEquals(List.of("follow-1", "similar-1"), response.items().stream().map(HomeFeedItem::contentId).toList());
	}

	static class RecordingSearchIndexReader implements SearchIndexReader {

		private String userId;

		private int size;

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
			this.userId = userId;
			this.size = size;
			HomeFeedItem followed = item("follow-1", FeedSourceBucket.FOLLOWED);
			HomeFeedItem trending = item("trend-1", FeedSourceBucket.TRENDING);
			HomeFeedItem freshDuplicate = item("trend-1", FeedSourceBucket.FRESH);
			HomeFeedItem similar = item("similar-1", FeedSourceBucket.SIMILAR);
			return new HomeFeedCandidates(List.of(followed), List.of(trending), List.of(freshDuplicate),
					List.of(similar));
		}

		private HomeFeedItem item(String contentId, FeedSourceBucket bucket) {
			return new HomeFeedItem(contentId, "chn-1", contentId, "desc", "VIDEO", "PUBLIC",
					Instant.parse("2026-03-14T12:00:00Z"), bucket);
		}
	}

	static class RecordingPolicyEvaluationClient implements PolicyEvaluationClient {

		private final List<String> recordedContentIds = new java.util.ArrayList<>();

		private List<String> blockedContentIds = List.of();

		@Override
		public PolicyDecision evaluate(String contentId, String countryCode, Boolean ageVerified) {
			recordedContentIds.add(contentId);
			boolean allowed = !blockedContentIds.contains(contentId);
			return new PolicyDecision(allowed, allowed ? List.of() : List.of("CONTENT_BLOCKED"));
		}
	}
}
