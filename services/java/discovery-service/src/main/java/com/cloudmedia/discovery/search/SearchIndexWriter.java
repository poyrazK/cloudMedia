package com.cloudmedia.discovery.search;

public interface SearchIndexWriter {

	void upsert(SearchDocument document);

	void delete(String contentId);
}
