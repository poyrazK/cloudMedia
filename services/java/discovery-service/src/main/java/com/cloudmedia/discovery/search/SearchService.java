package com.cloudmedia.discovery.search;

import com.cloudmedia.discovery.error.ApiException;
import com.cloudmedia.discovery.policy.PolicyEvaluationClient;
import com.cloudmedia.discovery.policy.PolicyEvaluationException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class SearchService {

	private static final int DEFAULT_PAGE = 0;

	static final int MIN_SIZE = 1;

	private static final int DEFAULT_SIZE = 20;

	private static final int DEFAULT_AUTOCOMPLETE_SIZE = 5;

	private static final int MAX_SIZE = 100;

	private static final int MAX_AUTOCOMPLETE_SIZE = 10;

	private final SearchIndexReader searchIndexReader;

	private final PolicyEvaluationClient policyEvaluationClient;

	public SearchService(SearchIndexReader searchIndexReader, PolicyEvaluationClient policyEvaluationClient) {
		this.searchIndexReader = searchIndexReader;
		this.policyEvaluationClient = policyEvaluationClient;
	}

	public SearchResponse search(String query, Integer page, Integer size, String countryCode, Boolean ageVerified) {
		int resolvedPage = page == null ? DEFAULT_PAGE : Math.max(page, 0);
		int resolvedSize = size == null ? DEFAULT_SIZE : Math.min(Math.max(size, MIN_SIZE), MAX_SIZE);
		SearchResponse searchResponse = searchIndexReader.search(query, resolvedPage, resolvedSize);
		List<SearchResultItem> filteredItems = filterByPolicy(searchResponse.items(), countryCode, ageVerified);
		return new SearchResponse(filteredItems, searchResponse.page(), searchResponse.size(), searchResponse.total());
	}

	public AutocompleteResponse autocomplete(String query, Integer size) {
		int resolvedSize = size == null
				? DEFAULT_AUTOCOMPLETE_SIZE
				: Math.min(Math.max(size, MIN_SIZE), MAX_AUTOCOMPLETE_SIZE);
		return searchIndexReader.autocomplete(query, resolvedSize);
	}

	private List<SearchResultItem> filterByPolicy(List<SearchResultItem> items, String countryCode,
			Boolean ageVerified) {
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
