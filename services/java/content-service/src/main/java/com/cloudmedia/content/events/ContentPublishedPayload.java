package com.cloudmedia.content.events;

import com.cloudmedia.content.persistence.entity.ContentType;
import com.cloudmedia.content.persistence.entity.ContentVisibility;
import java.time.LocalDateTime;

public record ContentPublishedPayload(String contentId, String channelId, String title, String description,
		ContentType contentType, ContentVisibility visibility, LocalDateTime publishedAt) {
}
