package com.cloudmedia.discovery.search;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchServiceTest {

	@Test
	void searchUsesDefaultPagingWhenParametersMissing() {
		RecordingSearchIndexReader reader = new RecordingSearchIndexReader();
		SearchService service = new SearchService(reader);

		SearchResponse response = service.search("video", null, null);

		assertEquals("video", reader.query);
		assertEquals(0, reader.page);
		assertEquals(20, reader.size);
		assertEquals(1, response.items().size());
	}

	@Test
	void searchClampsPagingValues() {
		RecordingSearchIndexReader reader = new RecordingSearchIndexReader();
		SearchService service = new SearchService(reader);

		service.search("video", -5, 1000);

		assertEquals(0, reader.page);
		assertEquals(100, reader.size);
	}

	@Test
	void searchClampsNonPositiveSize() {
		RecordingSearchIndexReader reader = new RecordingSearchIndexReader();
		SearchService service = new SearchService(reader);

		service.search("video", 1, 0);

		assertEquals(SearchService.MIN_SIZE, reader.size);
	}

	static class RecordingSearchIndexReader implements SearchIndexReader {

		private String query;

		private int page;

		private int size;

		@Override
		public SearchResponse search(String query, int page, int size) {
			this.query = query;
			this.page = page;
			this.size = size;
			return new SearchResponse(List.of(new SearchResultItem("cnt_1", "chn_1", "Title", "Description", "VIDEO",
					"PUBLIC", Instant.parse("2026-03-14T12:00:00Z"))), page, size, 1);
		}
	}
}
