package com.cloudmedia.discovery.search;

import java.time.Instant;

public record SearchResultItem(String contentId, String channelId, String title, String description, String contentType,
		String visibility, Instant publishedAt) {
}
