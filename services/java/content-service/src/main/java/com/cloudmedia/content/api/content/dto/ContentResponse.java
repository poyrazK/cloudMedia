package com.cloudmedia.content.api.content.dto;

import com.cloudmedia.content.persistence.entity.ContentState;
import com.cloudmedia.content.persistence.entity.ContentType;
import com.cloudmedia.content.persistence.entity.ContentVisibility;
import java.time.LocalDateTime;

public record ContentResponse(String id, String channelId, String title, String description, ContentType contentType,
		ContentState state, ContentVisibility visibility, boolean playbackReady, LocalDateTime createdAt,
		LocalDateTime updatedAt, LocalDateTime publishedAt, String thumbnailUrl) {
}
