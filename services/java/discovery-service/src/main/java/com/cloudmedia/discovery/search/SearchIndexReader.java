package com.cloudmedia.discovery.search;

public interface SearchIndexReader {

	SearchResponse search(String query, int page, int size);
}
