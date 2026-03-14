package com.cloudmedia.discovery.events;

public record ContentUpdatedPayload(String contentId, String channelId, String title, String contentType,
		String visibility) {
}
