package com.cloudmedia.discovery.events;

import com.cloudmedia.discovery.search.SearchDocument;
import com.cloudmedia.discovery.search.SearchDocumentUpdate;
import com.cloudmedia.discovery.search.SearchIndexWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ContentIndexEventListenerTest {

	private final RecordingSearchIndexWriter searchIndexWriter = new RecordingSearchIndexWriter();

	private ContentIndexEventListener listener;

	@BeforeEach
	void setUp() {
		listener = new ContentIndexEventListener(new ObjectMapper().findAndRegisterModules(), searchIndexWriter);
		searchIndexWriter.clear();
	}

	@Test
	void publishedEventUpsertsSearchDocument() {
		listener.handle("""
				{
				  "eventId": "evt_1",
				  "eventType": "content.published",
				  "eventVersion": 1,
				  "occurredAt": "2026-03-14T12:00:00Z",
				  "producer": "content-service",
				  "entityType": "content",
				  "entityId": "cnt_1",
				  "traceId": "req_1",
				  "payload": {
				    "contentId": "cnt_1",
				    "channelId": "chn_1",
				    "title": "Published title",
				    "description": "Published description",
				    "contentType": "VIDEO",
				    "visibility": "PUBLIC",
				    "publishedAt": "2026-03-14T12:00:00"
				  }
				}
				""");

		assertEquals(1, searchIndexWriter.upserts.size());
		assertEquals("cnt_1", searchIndexWriter.upserts.getFirst().contentId());
		assertEquals("Published description", searchIndexWriter.upserts.getFirst().description());
		assertEquals(Instant.parse("2026-03-14T12:00:00Z"), searchIndexWriter.upserts.getFirst().publishedAt());
	}

	@Test
	void updatedEventIssuesPartialUpdate() {
		listener.handle("""
				{
				  "eventId": "evt_2",
				  "eventType": "content.updated",
				  "eventVersion": 1,
				  "occurredAt": "2026-03-14T12:00:00Z",
				  "producer": "content-service",
				  "entityType": "content",
				  "entityId": "cnt_2",
				  "traceId": "req_2",
				  "payload": {
				    "contentId": "cnt_2",
				    "channelId": "chn_2",
				    "title": "Updated title",
				    "contentType": "VIDEO",
				    "visibility": "UNLISTED"
				  }
				}
				""");

		assertEquals(0, searchIndexWriter.upserts.size());
		assertEquals(1, searchIndexWriter.updates.size());
		assertEquals("cnt_2", searchIndexWriter.updates.getFirst().contentId());
		assertEquals("Updated title", searchIndexWriter.updates.getFirst().update().title());
	}

	@Test
	void unpublishedEventDeletesSearchDocument() {
		listener.handle("""
				{
				  "eventId": "evt_3",
				  "eventType": "content.unpublished",
				  "eventVersion": 1,
				  "occurredAt": "2026-03-14T12:00:00Z",
				  "producer": "content-service",
				  "entityType": "content",
				  "entityId": "cnt_3",
				  "traceId": "req_3",
				  "payload": {
				    "contentId": "cnt_3",
				    "channelId": "chn_3",
				    "previousState": "PUBLISHED",
				    "currentState": "PRIVATE",
				    "publishedAt": "2026-03-14T12:00:00"
				  }
				}
				""");

		assertEquals(List.of("cnt_3"), searchIndexWriter.deletes);
	}

	@Test
	void invalidJsonThrowsIllegalArgumentException() {
		assertThrows(IllegalArgumentException.class, () -> listener.handle("not valid json"));
	}

	@Test
	void unknownEventTypeThrowsIllegalArgumentException() {
		assertThrows(IllegalArgumentException.class, () -> listener.handle("""
				{
				  "eventId": "evt_4",
				  "eventType": "content.unknown",
				  "eventVersion": 1,
				  "occurredAt": "2026-03-14T12:00:00Z",
				  "producer": "content-service",
				  "entityType": "content",
				  "entityId": "cnt_4",
				  "traceId": "req_4",
				  "payload": {}
				}
				"""));
	}

	static class RecordingSearchIndexWriter implements SearchIndexWriter {

		private final List<SearchDocument> upserts = new ArrayList<>();

		private final List<UpdateCall> updates = new ArrayList<>();

		private final List<String> deletes = new ArrayList<>();

		@Override
		public void upsert(SearchDocument document) {
			upserts.add(document);
		}

		@Override
		public void update(String contentId, SearchDocumentUpdate update) {
			updates.add(new UpdateCall(contentId, update));
		}

		@Override
		public void delete(String contentId) {
			deletes.add(contentId);
		}

		void clear() {
			upserts.clear();
			updates.clear();
			deletes.clear();
		}
	}

	record UpdateCall(String contentId, SearchDocumentUpdate update) {
	}
}
