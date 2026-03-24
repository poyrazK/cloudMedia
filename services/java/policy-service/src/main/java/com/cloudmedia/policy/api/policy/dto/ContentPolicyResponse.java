package com.cloudmedia.policy.api.policy.dto;

import com.cloudmedia.policy.persistence.entity.ModerationState;
import java.time.Instant;
import java.util.List;

public record ContentPolicyResponse(String contentId, boolean ageRestricted, List<String> geoAllowList,
		List<String> geoBlockList, ModerationState moderationState, Instant createdAt, Instant updatedAt) {
}
