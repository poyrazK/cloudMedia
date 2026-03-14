package com.cloudmedia.discovery.search;

public interface SearchIndexWriter {

	void upsert(SearchDocument document);

	void update(String contentId, SearchDocumentUpdate update);

	void delete(String contentId);
}
