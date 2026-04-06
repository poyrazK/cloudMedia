package com.cloudmedia.content.events;

import java.time.LocalDateTime;

public record ContentPublishedPayload(String contentId, String channelId, String title, String description,
		String contentType, String visibility, LocalDateTime publishedAt) {
}
