package com.cloudmedia.policy.api.policy.dto;

import com.cloudmedia.policy.persistence.entity.ModerationState;
import java.util.List;

public record ContentPolicyDecisionResponse(String contentId, boolean allowed, List<String> reasonCodes,
		ModerationState moderationState, boolean ageRestricted, List<String> geoAllowList, List<String> geoBlockList) {

	public ContentPolicyDecisionResponse {
		reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
		geoAllowList = geoAllowList == null ? List.of() : List.copyOf(geoAllowList);
		geoBlockList = geoBlockList == null ? List.of() : List.copyOf(geoBlockList);
	}
}
