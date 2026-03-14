package com.cloudmedia.discovery.search;

import java.time.Instant;

public record SearchDocument(String contentId, String channelId, String title, String description, String contentType,
		String visibility, Instant publishedAt) {
}
