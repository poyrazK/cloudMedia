package com.cloudmedia.content.api.channel.dto;

import java.time.LocalDateTime;

public record ChannelResponse(String id, String slug, String displayName, String description, LocalDateTime createdAt,
		LocalDateTime updatedAt) {
}
