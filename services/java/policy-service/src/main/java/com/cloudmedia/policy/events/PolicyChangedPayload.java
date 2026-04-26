package com.cloudmedia.policy.events;

import java.time.Instant;
import java.util.List;

public record PolicyChangedPayload(String contentId, boolean ageRestricted, List<String> geoAllowList,
		List<String> geoBlockList, String moderationState, Instant occurredAt) {
}
