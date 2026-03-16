package com.cloudmedia.discovery.search;

import com.cloudmedia.discovery.discovery.HomeFeedCandidates;
import java.util.List;

public class NoopSearchIndexReader implements SearchIndexReader {

	@Override
	public SearchResponse search(String query, int page, int size) {
		return new SearchResponse(List.of(), page, size, 0);
	}

	@Override
	public AutocompleteResponse autocomplete(String query, int size) {
		return new AutocompleteResponse(List.of(), size);
	}

	@Override
	public HomeFeedCandidates homeFeed(String userId, int size) {
		return HomeFeedCandidates.empty();
	}
}
