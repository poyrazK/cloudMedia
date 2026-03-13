package com.cloudmedia.content.api.content.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PlaybackResponse(String contentId, String manifestUrl, boolean playbackReady,
		List<PlaybackRenditionResponse> availableRenditions, LocalDateTime publishedAt) {
}
