package com.cloudmedia.discovery.events;

import com.cloudmedia.discovery.search.SearchDocument;
import com.cloudmedia.discovery.search.SearchIndexWriter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;

public class ContentIndexEventListener {

	private final ObjectMapper objectMapper;

	private final SearchIndexWriter searchIndexWriter;

	public ContentIndexEventListener(ObjectMapper objectMapper, SearchIndexWriter searchIndexWriter) {
		this.objectMapper = objectMapper;
		this.searchIndexWriter = searchIndexWriter;
	}

	@KafkaListener(groupId = "${cloudmedia.discovery.kafka.group-id}", topics = {
			"${cloudmedia.discovery.kafka.topics.content-published}",
			"${cloudmedia.discovery.kafka.topics.content-updated}",
			"${cloudmedia.discovery.kafka.topics.content-unpublished}"})
	public void handle(String rawEvent) {
		ContentEventEnvelope envelope = readEnvelope(rawEvent);
		switch (envelope.eventType()) {
			case "content.published" -> searchIndexWriter.upsert(toPublishedDocument(envelope));
			case "content.updated" -> searchIndexWriter.upsert(toUpdatedDocument(envelope));
			case "content.unpublished" ->
				searchIndexWriter.delete(readPayload(envelope, ContentUnpublishedPayload.class).contentId());
			default -> throw new IllegalArgumentException("Unsupported content event type " + envelope.eventType());
		}
	}

	private ContentEventEnvelope readEnvelope(String rawEvent) {
		try {
			return objectMapper.readValue(rawEvent, ContentEventEnvelope.class);
		} catch (JsonProcessingException exception) {
			throw new IllegalArgumentException("Failed to parse content event envelope", exception);
		}
	}

	private SearchDocument toPublishedDocument(ContentEventEnvelope envelope) {
		ContentPublishedPayload payload = readPayload(envelope, ContentPublishedPayload.class);
		return new SearchDocument(payload.contentId(), payload.channelId(), payload.title(), payload.description(),
				payload.contentType(), payload.visibility(), payload.publishedAt());
	}

	private SearchDocument toUpdatedDocument(ContentEventEnvelope envelope) {
		ContentUpdatedPayload payload = readPayload(envelope, ContentUpdatedPayload.class);
		return new SearchDocument(payload.contentId(), payload.channelId(), payload.title(), null,
				payload.contentType(), payload.visibility(), null);
	}

	private <T> T readPayload(ContentEventEnvelope envelope, Class<T> payloadType) {
		return objectMapper.convertValue(envelope.payload(), payloadType);
	}
}
