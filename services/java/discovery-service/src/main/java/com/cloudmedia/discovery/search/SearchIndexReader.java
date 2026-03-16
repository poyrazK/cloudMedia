package com.cloudmedia.discovery.search;

import com.cloudmedia.discovery.discovery.HomeFeedCandidates;

public interface SearchIndexReader {

	SearchResponse search(String query, int page, int size);

	AutocompleteResponse autocomplete(String query, int size);

	HomeFeedCandidates homeFeed(String userId, int size);
}
