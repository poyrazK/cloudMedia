package com.cloudmedia.policy.api.policy.dto;

import com.cloudmedia.policy.persistence.entity.ModerationState;
import java.util.List;

public record ContentPolicyDecisionResponse(String contentId, boolean allowed, List<String> reasonCodes,
		ModerationState moderationState, boolean ageRestricted, List<String> geoAllowList, List<String> geoBlockList) {
}
