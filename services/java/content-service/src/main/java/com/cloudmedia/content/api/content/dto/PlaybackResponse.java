package com.cloudmedia.content.api.content.dto;

import java.util.List;

public record PlaybackResponse(String contentId, String manifestUrl, List<String> availableRenditions) {
}
