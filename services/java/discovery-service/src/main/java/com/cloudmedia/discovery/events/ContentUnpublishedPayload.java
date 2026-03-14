package com.cloudmedia.discovery.events;

import java.time.LocalDateTime;

public record ContentUnpublishedPayload(String contentId, String channelId, String previousState, String currentState,
		LocalDateTime publishedAt) {
}
