package com.cloudmedia.discovery.search;

public record SearchDocumentUpdate(String channelId, String title, String description, String contentType,
		String visibility) {
}
