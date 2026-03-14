package com.cloudmedia.content.events;

import com.cloudmedia.content.persistence.entity.ContentState;
import java.time.LocalDateTime;

public record ContentUnpublishedPayload(String contentId, String channelId, ContentState previousState,
		ContentState currentState, LocalDateTime publishedAt) {
}
