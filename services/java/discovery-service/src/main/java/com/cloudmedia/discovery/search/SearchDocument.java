package com.cloudmedia.discovery.search;

import java.time.LocalDateTime;

public record SearchDocument(String contentId, String channelId, String title, String description, String contentType,
		String visibility, LocalDateTime publishedAt) {
}
