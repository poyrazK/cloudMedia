package com.cloudmedia.content.events;

import com.cloudmedia.content.persistence.entity.ContentType;
import com.cloudmedia.content.persistence.entity.ContentVisibility;

public record ContentUpdatedPayload(String contentId, String channelId, String title, ContentType contentType,
		ContentVisibility visibility) {
}
