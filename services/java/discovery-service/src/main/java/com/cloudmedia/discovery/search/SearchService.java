package com.cloudmedia.discovery.search;

import org.springframework.stereotype.Service;

@Service
public class SearchService {

	private static final int DEFAULT_PAGE = 0;

	private static final int DEFAULT_SIZE = 20;

	private static final int MAX_SIZE = 100;

	private final SearchIndexReader searchIndexReader;

	public SearchService(SearchIndexReader searchIndexReader) {
		this.searchIndexReader = searchIndexReader;
	}

	public SearchResponse search(String query, Integer page, Integer size) {
		int resolvedPage = page == null ? DEFAULT_PAGE : Math.max(page, 0);
		int resolvedSize = size == null ? DEFAULT_SIZE : Math.min(Math.max(size, 1), MAX_SIZE);
		return searchIndexReader.search(query, resolvedPage, resolvedSize);
	}
}
