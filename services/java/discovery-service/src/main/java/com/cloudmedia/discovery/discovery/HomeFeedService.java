package com.cloudmedia.discovery.discovery;

import com.cloudmedia.discovery.error.ApiException;
import com.cloudmedia.discovery.policy.PolicyEvaluationClient;
import com.cloudmedia.discovery.policy.PolicyEvaluationException;
import com.cloudmedia.discovery.search.SearchIndexReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class HomeFeedService {

	private static final int MIN_SIZE = 1;

	private static final int DEFAULT_SIZE = 20;

	private static final int MAX_SIZE = 50;

	private final SearchIndexReader searchIndexReader;

	private final PolicyEvaluationClient policyEvaluationClient;

	public HomeFeedService(SearchIndexReader searchIndexReader, PolicyEvaluationClient policyEvaluationClient) {
		this.searchIndexReader = searchIndexReader;
		this.policyEvaluationClient = policyEvaluationClient;
	}

	public HomeFeedResponse homeFeed(String userId, Integer size, String countryCode, Boolean ageVerified) {
		int resolvedSize = size == null ? DEFAULT_SIZE : Math.min(Math.max(size, MIN_SIZE), MAX_SIZE);
		HomeFeedCandidates candidates = searchIndexReader.homeFeed(userId, resolvedSize);
		HomeFeedCandidates policyFilteredCandidates = new HomeFeedCandidates(
				filterByPolicy(candidates.followed(), countryCode, ageVerified),
				filterByPolicy(candidates.trending(), countryCode, ageVerified),
				filterByPolicy(candidates.fresh(), countryCode, ageVerified),
				filterByPolicy(candidates.similar(), countryCode, ageVerified));
		return new HomeFeedResponse(blend(policyFilteredCandidates, resolvedSize), resolvedSize);
	}

	private List<HomeFeedItem> blend(HomeFeedCandidates candidates, int size) {
		Map<String, HomeFeedItem> deduped = new LinkedHashMap<>();
		addBucket(deduped, candidates.followed(), slotsFor(size, 0.4d));
		addBucket(deduped, candidates.trending(), slotsFor(size, 0.3d));
		addBucket(deduped, candidates.fresh(), slotsFor(size, 0.2d));
		addBucket(deduped, candidates.similar(), slotsFor(size, 0.1d));
		fillRemaining(deduped, candidates, size);
		return new ArrayList<>(deduped.values()).subList(0, Math.min(size, deduped.size()));
	}

	private void addBucket(Map<String, HomeFeedItem> deduped, List<HomeFeedItem> items, int maxItems) {
		if (maxItems <= 0) {
			return;
		}
		int added = 0;
		for (HomeFeedItem item : items) {
			if (item.contentId() == null || deduped.containsKey(item.contentId())) {
				continue;
			}
			deduped.put(item.contentId(), item);
			added++;
			if (added >= maxItems) {
				return;
			}
		}
	}

	private void fillRemaining(Map<String, HomeFeedItem> deduped, HomeFeedCandidates candidates, int size) {
		List<HomeFeedItem> all = new ArrayList<>();
		all.addAll(candidates.followed());
		all.addAll(candidates.trending());
		all.addAll(candidates.fresh());
		all.addAll(candidates.similar());
		for (HomeFeedItem item : all) {
			if (deduped.size() >= size) {
				return;
			}
			if (item.contentId() != null) {
				deduped.putIfAbsent(item.contentId(), item);
			}
		}
	}

	private int slotsFor(int size, double ratio) {
		return Math.max(1, (int) Math.floor(size * ratio));
	}

	private List<HomeFeedItem> filterByPolicy(List<HomeFeedItem> items, String countryCode, Boolean ageVerified) {
		try {
			return items.stream().filter(
					item -> policyEvaluationClient.evaluate(item.contentId(), countryCode, ageVerified).allowed())
					.toList();
		} catch (PolicyEvaluationException exception) {
			throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "POLICY_SERVICE_UNAVAILABLE",
					"Policy evaluation is temporarily unavailable", null);
		}
	}
}
