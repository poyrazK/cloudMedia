package com.cloudmedia.discovery.discovery;

import java.time.Instant;

public record HomeFeedItem(String contentId, String channelId, String title, String description, String contentType,
		String visibility, Instant publishedAt, FeedSourceBucket sourceBucket) {
}
