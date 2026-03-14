package com.cloudmedia.discovery.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoopSearchIndexWriter implements SearchIndexWriter {

	private static final Logger LOGGER = LoggerFactory.getLogger(NoopSearchIndexWriter.class);

	@Override
	public void upsert(SearchDocument document) {
		if (document == null) {
			LOGGER.debug("Noop index writer skipped null upsert document");
			return;
		}
		LOGGER.debug("Noop index writer skipped upsert for contentId={}", document.contentId());
	}

	@Override
	public void update(String contentId, SearchDocumentUpdate update) {
		LOGGER.debug("Noop index writer skipped partial update for contentId={}", contentId);
	}

	@Override
	public void delete(String contentId) {
		LOGGER.debug("Noop index writer skipped delete for contentId={}", contentId);
	}
}
